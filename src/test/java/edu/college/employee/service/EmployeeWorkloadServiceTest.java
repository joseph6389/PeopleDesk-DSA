package edu.college.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.college.employee.model.Employee;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmployeeWorkloadServiceTest {
    @TempDir Path temp;

    @Test
    void initializesMetricsAddsTasksWeightsAndPersistsOneGreedyRunAcrossRestart() throws IOException {
        List<Employee> employees = new ArrayList<>(List.of(employee("EMP002", "ACTIVE"), employee("EMP001", "ACTIVE"), employee("EMP003", "INACTIVE")));
        EmployeeWorkloadService service = new EmployeeWorkloadService(() -> List.copyOf(employees), temp);
        assertTrue(Files.exists(service.workloadFile()));
        assertTrue(Files.exists(service.historyFile()));
        assertEquals(3, service.view().employees().size());
        assertEquals(0, service.view().employees().get(0).workload().currentWorkloadUnits());

        service.addTask("Attendance report", "", "Easy");
        service.addTask("Database update", "Optional details", "Medium");
        var allocation = service.allocate();
        assertEquals(List.of("EMP001", "EMP002"), allocation.latestRun().tasks().stream()
                .map(EmployeeWorkloadService.AllocationItem::assignedEmployeeId).toList());
        assertEquals(List.of(1, 2), allocation.latestRun().tasks().stream()
                .map(EmployeeWorkloadService.AllocationItem::weight).toList());
        var attendance = allocation.latestRun().tasks().get(0);
        assertEquals("EMP001", attendance.assignedEmployeeId());
        assertEquals("Employee EMP001", attendance.assignedEmployeeName());
        assertEquals("Operations", attendance.assignedDepartment());
        assertTrue(attendance.tieBreakApplied());
        assertTrue(attendance.reason().contains("Employee ID was lowest"));
        assertEquals(List.of("EMP002", "EMP001"), attendance.eligibleWorkloads().stream()
                .map(edu.college.employee.dsa.GreedyWorkloadAllocator.EligibleWorkload::employeeId).toList());
        assertTrue(allocation.pendingTasks().isEmpty());
        assertEquals(1, allocation.history().size());

        var json = new ObjectMapper().readTree(service.workloadFile().toFile());
        assertEquals(3, json.size());
        assertFalse(json.get("emp001").has("name"));
        assertEquals(1, json.get("emp001").get("currentWorkloadUnits").asInt());

        EmployeeWorkloadService restarted = new EmployeeWorkloadService(() -> List.copyOf(employees), temp);
        assertEquals(allocation.latestRun(), restarted.view().latestRun());
        assertEquals(1, restarted.view().history().size());
        assertEquals(1, restarted.view().employees().stream().filter(item -> item.employeeId().equals("EMP001"))
                .findFirst().orElseThrow().workload().assignedTaskCount());
        assertThrows(IllegalArgumentException.class, restarted::allocate);
        assertEquals(1, restarted.view().history().size());
    }

    @Test
    void newEmployeesGetInitialWorkloadRecordsAndResetPreservesHistoryAndTaskCount() throws IOException {
        List<Employee> employees = new ArrayList<>(List.of(employee("EMP001", "ACTIVE")));
        EmployeeWorkloadService service = new EmployeeWorkloadService(() -> List.copyOf(employees), temp);
        service.addTask("Audit", "", "Difficult");
        service.allocate();
        employees.add(employee("EMP002", "ACTIVE"));

        var view = service.view();
        assertEquals(0, view.employees().stream().filter(item -> item.employeeId().equals("EMP002"))
                .findFirst().orElseThrow().workload().currentWorkloadUnits());
        var reset = service.resetWorkload("EMP001");
        var employee = reset.employees().stream().filter(item -> item.employeeId().equals("EMP001")).findFirst().orElseThrow();
        assertEquals(0, employee.workload().currentWorkloadUnits());
        assertEquals(1, employee.workload().assignedTaskCount());
        assertEquals(1, reset.history().size());
    }

    @Test
    void noActiveEmployeesKeepsPendingTasksAndDoesNotCreateAFalseAllocationRun() throws IOException {
        EmployeeWorkloadService service = new EmployeeWorkloadService(() -> List.of(employee("EMP001", "ON_LEAVE")), temp);
        service.addTask("Prepare report", "", "Medium");
        assertThrows(IllegalArgumentException.class, service::allocate);
        assertEquals(1, service.view().pendingTasks().size());
        assertTrue(service.view().history().isEmpty());
        assertEquals(0, service.view().employees().get(0).workload().currentWorkloadUnits());
    }

    @Test
    void simulationUsesGreedyPreviewAndLeavesWorkloadHistoryAndPendingTasksUnchanged() throws IOException {
        EmployeeWorkloadService service = new EmployeeWorkloadService(
                () -> List.of(employee("EMP002", "ACTIVE"), employee("EMP001", "ACTIVE")), temp);
        service.addTask("Preview easy", "", "Easy");
        service.addTask("Preview difficult", "", "Difficult");
        byte[] workloadsBefore = Files.readAllBytes(service.workloadFile());
        byte[] historyBefore = Files.readAllBytes(service.historyFile());

        var plan = service.simulate();

        assertEquals(List.of("EMP001", "EMP002"), plan.tasks().stream()
                .map(EmployeeWorkloadService.AllocationItem::assignedEmployeeId).toList());
        assertEquals(List.of(0, 0), plan.tasks().stream()
                .map(EmployeeWorkloadService.AllocationItem::previousWorkload).toList());
        assertEquals(2, service.view().pendingTasks().size());
        assertTrue(service.view().history().isEmpty());
        assertEquals(java.util.Arrays.toString(workloadsBefore), java.util.Arrays.toString(Files.readAllBytes(service.workloadFile())));
        assertEquals(java.util.Arrays.toString(historyBefore), java.util.Arrays.toString(Files.readAllBytes(service.historyFile())));
    }

    @Test
    void readsExistingAllocationHistoryWithoutNewDecisionDetails() throws IOException {
        Path historyFile = temp.resolve("workload-allocation-history.json");
        Files.writeString(historyFile, """
                [{"runId":"old-run","allocatedAt":"2026-01-01T00:00:00Z","totalWorkUnits":1,
                  "totalWorkloadWeight":1,"totalAllocatedTasks":1,"totalUnallocatedTasks":0,
                  "eligibleEmployeeCount":1,"tasks":[{"taskId":"TASK-1","taskName":"Legacy task",
                  "difficulty":"Easy","weight":1,"assignedEmployeeId":"EMP001","previousWorkload":0,
                  "updatedWorkload":1,"reason":""}],"before":[],"after":[]}]
                """);

        EmployeeWorkloadService service = new EmployeeWorkloadService(() -> List.of(employee("EMP001", "ACTIVE")), temp);

        var task = service.view().history().get(0).tasks().get(0);
        assertEquals("EMP001", task.assignedEmployeeId());
        assertEquals(List.of(), task.eligibleWorkloads());
    }

    @Test
    void invalidWorkloadJsonIsNotOverwrittenOrMoved() throws IOException {
        Path file = temp.resolve("employee-workload.json");
        Files.writeString(file, "{broken");

        IOException error = assertThrows(IOException.class,
                () -> new EmployeeWorkloadService(() -> List.of(), temp));

        assertTrue(error.getMessage().contains("employee-workload.json"));
        assertEquals("{broken", Files.readString(file));
        try (var paths = Files.list(temp)) { assertEquals(List.of(file), paths.toList()); }
    }

    @Test
    void validatesTasksAndDoesNotPersistInvalidDifficultyOrNames() throws IOException {
        EmployeeWorkloadService service = new EmployeeWorkloadService(() -> List.of(), temp);
        assertThrows(IllegalArgumentException.class, () -> service.addTask("", "", "Easy"));
        assertThrows(IllegalArgumentException.class, () -> service.addTask("Work", "", "Extreme"));
        service.addTask("Work", "", "Easy");
        assertThrows(java.util.NoSuchElementException.class, () -> service.removeTask("missing"));
        assertEquals(1, service.view().pendingTasks().size());
    }

    private static Employee employee(String id, String status) {
        return new Employee(id, "Employee " + id, "Operations", "Coordinator", id.toLowerCase() + "@example.test",
                "555-0100", LocalDate.parse("2024-01-01"), status, new BigDecimal("50000.00"));
    }
}
