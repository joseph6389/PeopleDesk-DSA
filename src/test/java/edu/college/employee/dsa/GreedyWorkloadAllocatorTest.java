package edu.college.employee.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.college.employee.model.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GreedyWorkloadAllocatorTest {
    private final GreedyWorkloadAllocator allocator = new GreedyWorkloadAllocator();

    @Test
    void assignsTheLowestCurrentWorkloadAndRecalculatesAfterEachTask() {
        var employees = List.of(employee("EMP002", "ACTIVE"), employee("EMP001", "ACTIVE"), employee("EMP003", "ACTIVE"));
        var tasks = List.of(task("A", "Easy", 1), task("B", "Medium", 2));

        var result = allocator.allocate(employees, Map.of("emp002", 0, "emp001", 1, "emp003", 4), tasks);

        assertEquals(List.of("EMP002", "EMP001"), result.assignments().stream().map(GreedyWorkloadAllocator.Assignment::employeeId).toList());
        assertEquals(List.of(0, 1), result.assignments().stream().map(GreedyWorkloadAllocator.Assignment::previousWorkload).toList());
        assertEquals(List.of(1, 3), result.assignments().stream().map(GreedyWorkloadAllocator.Assignment::updatedWorkload).toList());
        assertEquals(List.of(0, 1, 4), result.assignments().get(0).eligibleWorkloads().stream()
                .map(GreedyWorkloadAllocator.EligibleWorkload::workload).toList());
        assertEquals("Operations", result.assignments().get(0).department());
        assertFalse(result.assignments().get(0).tieBreakApplied());
    }

    @Test
    void mapsEasyMediumAndDifficultToTheirConfiguredWeights() {
        var result = allocator.allocate(List.of(employee("EMP001", "ACTIVE")), Map.of(),
                List.of(task("A", "Easy", 1), task("B", "Medium", 2), task("C", "Difficult", 3)));

        assertEquals(List.of(1, 2, 3), result.assignments().stream().map(assignment -> assignment.task().weight()).toList());
        assertEquals(List.of(1, 3, 6), result.assignments().stream().map(GreedyWorkloadAllocator.Assignment::updatedWorkload).toList());
    }

    @Test
    void tiesUseEmployeeIdAscendingAndPermitRepeatedAssignments() {
        var result = allocator.allocate(List.of(employee("EMP002", "ACTIVE"), employee("EMP001", "ACTIVE")), Map.of(),
                List.of(task("A", "Easy", 1), task("B", "Easy", 1), task("C", "Easy", 1)));

        assertEquals(List.of("EMP001", "EMP002", "EMP001"),
                result.assignments().stream().map(GreedyWorkloadAllocator.Assignment::employeeId).toList());
        assertEquals(List.of(true, false, true),
                result.assignments().stream().map(GreedyWorkloadAllocator.Assignment::tieBreakApplied).toList());
    }

    @Test
    void excludesInactiveAndOnLeaveEmployees() {
        var result = allocator.allocate(List.of(employee("EMP001", "INACTIVE"), employee("EMP002", "ON_LEAVE"),
                        employee("EMP003", "ACTIVE")), Map.of(), List.of(task("A", "Easy", 1)));
        assertEquals(List.of("EMP003"), result.assignments().stream().map(GreedyWorkloadAllocator.Assignment::employeeId).toList());
    }

    @Test
    void noActiveEmployeesProducesUnallocatedTasksAndEmptyTasksProducesNoAssignments() {
        var noWorkers = allocator.allocate(List.of(employee("EMP001", "ON_LEAVE")), Map.of(), List.of(task("A", "Difficult", 3)));
        assertTrue(noWorkers.assignments().isEmpty());
        assertEquals("No ACTIVE employees are available.", noWorkers.unallocated().get(0).reason());

        var noTasks = allocator.allocate(List.of(employee("EMP001", "ACTIVE")), Map.of(), List.of());
        assertTrue(noTasks.assignments().isEmpty());
        assertTrue(noTasks.unallocated().isEmpty());
    }

    @Test
    void rejectsInvalidDifficultyWeightsAndNegativeStoredWorkloads() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> task("A", "Easy", 2));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> allocator.allocate(List.of(employee("EMP001", "ACTIVE")), Map.of("emp001", -1), List.of()));
    }

    private static GreedyWorkloadAllocator.WorkUnit task(String id, String difficulty, int weight) {
        return new GreedyWorkloadAllocator.WorkUnit(id, "Task " + id, difficulty, weight);
    }

    private static Employee employee(String id, String status) {
        return new Employee(id, "Employee " + id, "Operations", "Coordinator", id.toLowerCase() + "@example.test",
                "555-0100", LocalDate.parse("2024-01-01"), status, new BigDecimal("50000.00"));
    }
}
