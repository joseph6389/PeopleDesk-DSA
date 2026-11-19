package edu.college.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.college.employee.dsa.EmployeeBinaryTree;
import edu.college.employee.model.EmployeeRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmployeeLeaveQueueTest {
    @TempDir Path temp;

    @Test
    void pendingLeaveQueueIsFifoAndKeepsProcessedHistoryAcrossRestart() throws IOException {
        AppData data = repository("leave-flow");
        EmployeeRequest first = leave("LEAVE-1", "2026-12-01T10:00:00Z");
        EmployeeRequest second = leave("LEAVE-2", "2026-12-01T10:01:00Z");
        data.submitRequest(first);
        data.submitRequest(second);
        assertEquals(2, data.pendingLeaveCount());
        assertEquals(first, data.oldestPendingLeaveRequest());
        assertThrows(IllegalArgumentException.class,
                () -> data.processLeaveRequest(second.id(), "APPROVED", "hr-admin", ""));
        assertEquals(first, data.oldestPendingLeaveRequest());

        EmployeeRequest approved = data.processLeaveRequest(first.id(), "APPROVED", "hr-admin", "Approved");
        assertEquals("APPROVED", approved.status());
        assertEquals(first.createdAt(), approved.createdAt());
        assertEquals(first.leaveFrom(), approved.leaveFrom());
        assertEquals(first.leaveTo(), approved.leaveTo());
        assertEquals(first.details(), approved.details());
        assertEquals(second, data.oldestPendingLeaveRequest());
        assertEquals(1, data.pendingLeaveCount());

        AppData restarted = repository("leave-flow");
        assertEquals(second, restarted.oldestPendingLeaveRequest());
        assertEquals(2, restarted.requests().size());
        EmployeeRequest rejected = restarted.processLeaveRequest(second.id(), "REJECTED", "hr-admin", "Not available");
        assertEquals("REJECTED", rejected.status());
        assertTrue(restarted.employeeRequestsFile().toFile().isFile());
        assertEquals(0, restarted.pendingLeaveCount());
        assertNull(restarted.oldestPendingLeaveRequest());

        AppData afterDecisions = repository("leave-flow");
        assertEquals(0, afterDecisions.pendingLeaveCount());
        assertEquals(List.of("APPROVED", "REJECTED"), afterDecisions.requests().stream()
                .map(EmployeeRequest::status).toList());
    }

    @Test
    void rejectsDuplicateIdsAndDoesNotQueueNonLeaveRequests() throws IOException {
        AppData data = repository("validation");
        data.submitRequest(leave("DUPLICATE", "2026-12-01T10:00:00Z"));
        assertThrows(IllegalArgumentException.class,
                () -> data.submitRequest(leave("DUPLICATE", "2026-12-01T10:01:00Z")));
        data.submitRequest(new EmployeeRequest("GENERAL-1", "EMP001", "General", "Question", "PENDING",
                "employee", Instant.parse("2026-12-01T10:02:00Z"), null, null, null, null, null));
        assertEquals(1, data.pendingLeaveCount());
        assertEquals("DUPLICATE", data.oldestPendingLeaveRequest().id());
        assertEquals(2, data.requests().size());
        AppData restarted = repository("validation");
        assertEquals(2, restarted.requests().size());
        assertEquals(1, restarted.pendingLeaveCount());
    }

    @Test
    void persistenceFailureLeavesRequestAndQueueUnchanged() throws IOException {
        Path blocker = temp.resolve("not-a-directory");
        Files.writeString(blocker, "occupied");
        AppData data = repository("failure", blocker.resolve("employee-requests.json"));

        assertThrows(UncheckedIOException.class, () -> data.submitRequest(leave("FAIL-1", "2026-12-01T10:00:00Z")));
        assertTrue(data.requests().isEmpty());
        assertEquals(0, data.pendingLeaveCount());
        assertNull(data.oldestPendingLeaveRequest());
    }

    @Test
    void failedDecisionSaveLeavesTheOldestApplicationInTheQueue() throws IOException {
        Path historyDirectory = temp.resolve("decision-failure");
        Path historyFile = historyDirectory.resolve("employee-requests.json");
        AppData data = repository("decision-failure", historyFile);
        EmployeeRequest request = leave("LEAVE-SAVE-FAIL", "2026-12-01T10:00:00Z");
        data.submitRequest(request);
        Files.delete(historyFile);
        Files.delete(historyDirectory);
        Files.writeString(historyDirectory, "block directory creation");

        assertThrows(IOException.class,
                () -> data.processLeaveRequest(request.id(), "APPROVED", "hr-admin", "Approved"));
        assertEquals(request, data.oldestPendingLeaveRequest());
        assertEquals(1, data.pendingLeaveCount());
        assertEquals("PENDING", data.requests().get(0).status());
    }

    private AppData repository(String name) throws IOException {
        return repository(name, temp.resolve(name).resolve("employee-requests.json"));
    }

    private AppData repository(String name, Path requests) throws IOException {
        Path directory = temp.resolve(name + "-tree");
        Files.createDirectories(directory);
        Path treeFile = directory.resolve("employee-tree.json");
        if (Files.notExists(treeFile)) Files.copy(App.employeeBinaryTreeFile(), treeFile);
        return new AppData(new EmployeeBinaryTree(treeFile), requests);
    }

    private static EmployeeRequest leave(String id, String createdAt) {
        return new EmployeeRequest(id, "EMP001", "Leave", "Family leave", "PENDING", "employee",
                Instant.parse(createdAt), null, null, null, "2026-12-10", "2026-12-12");
    }
}
