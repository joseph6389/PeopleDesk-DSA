package edu.college.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.college.employee.dsa.EmployeeBinaryTree;
import edu.college.employee.model.Employee;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmployeeStorageSynchronizationTest {
    @TempDir Path temp;

    @Test
    void loadsRealEmployeesIntoBothStructuresAndKeepsCrudAndRestartSynchronized() throws IOException {
        Path storage = isolatedEmployeeTree();
        AppData data = new AppData(new EmployeeBinaryTree(storage), requestFile(storage));

        assertSynchronized(data);
        int originalCount = data.employees().size();
        assertEquals(52, originalCount);
        assertEquals(data.searchEmployeeInList("emp001"), data.searchEmployeeInTree("EMP001"));
        assertEquals(data.searchEmployeeInTree("emp052"), data.searchEmployeeInBst("EMP052"));
        assertNull(data.searchEmployeeInList("missing"));
        assertNull(data.searchEmployeeInTree("missing"));

        Employee added = employee("SYNC-TEST-INSERT-001", "New Employee", "new.employee@example.test");
        data.insertEmployeeInTree(added);
        assertSynchronized(data);
        assertEquals(originalCount + 1, data.employees().size());
        assertEquals(added, data.searchEmployeeInList("sync-test-insert-001"));
        assertEquals(added, data.searchEmployeeInTree("SYNC-TEST-INSERT-001"));
        assertEquals(added, data.searchEmployeeInBst("SYNC-TEST-INSERT-001"));
        assertThrows(IllegalArgumentException.class, () -> data.insertEmployeeInTree(added));

        Employee old = data.searchEmployeeInList("EMP002");
        Employee updated = new Employee(old.id(), "Updated Ishita Patel", old.department(), old.designation(),
                "ishita.updated@example.test", old.phone(), old.joiningDate(), old.status(), old.salary());
        data.updateEmployeeInTree(old.id(), updated);
        assertSynchronized(data);
        assertEquals(updated, data.searchEmployeeInList(old.id()));
        assertEquals(updated, data.searchEmployeeInTree(old.id()));
        assertEquals(updated, data.searchEmployeeInBst(old.id()));

        Employee duplicateEmail = data.searchEmployeeInList("EMP003");
        Employee invalidUpdate = new Employee(duplicateEmail.id(), duplicateEmail.name(), duplicateEmail.department(),
                duplicateEmail.designation(), updated.email(), duplicateEmail.phone(), duplicateEmail.joiningDate(),
                duplicateEmail.status(), duplicateEmail.salary());
        assertThrows(IllegalArgumentException.class, () -> data.updateEmployeeInTree(duplicateEmail.id(), invalidUpdate));
        assertSynchronized(data);

        data.deleteEmployeeFromTree("EMP020");
        assertSynchronized(data);
        assertNull(data.searchEmployeeInList("EMP020"));
        assertNull(data.searchEmployeeInTree("EMP020"));
        assertNull(data.searchEmployeeInBst("EMP020"));
        assertEquals(originalCount, data.employees().size());

        List<Employee> persisted = data.employees();
        AppData restarted = new AppData(new EmployeeBinaryTree(storage), requestFile(storage));
        assertEquals(persisted, restarted.employees());
        assertSynchronized(restarted);
        assertEquals(updated, restarted.searchEmployeeInList("EMP002"));
    }

    @Test
    void employeeManagementMutationsKeepTheLinkedListAndTreeInStep() throws IOException {
        Path storage = isolatedEmployeeTree();
        AppData data = new AppData(new EmployeeBinaryTree(storage), requestFile(storage));
        Employee added = employee("EMP060", "List Path Employee", "list.path@example.test");

        data.changeEmployees(records -> { records.add(added); return null; });
        assertSynchronized(data);
        assertEquals(added, data.searchEmployeeInList(added.id()));
        assertEquals(added, data.searchEmployeeInTree(added.id()));
        assertEquals(added, data.searchEmployeeInBst(added.id()));

        Employee renamed = new Employee(added.id(), "Renamed Employee", added.department(), added.designation(),
                added.email(), added.phone(), added.joiningDate(), added.status(), added.salary());
        data.changeEmployees(records -> {
            for (int i = 0; i < records.size(); i++) if (records.get(i).id().equals(added.id())) records.set(i, renamed);
            return null;
        });
        assertSynchronized(data);
        assertEquals(renamed, data.searchEmployeeInList(added.id()));
        assertEquals(renamed, data.searchEmployeeInTree(added.id()));
        assertEquals(renamed, data.searchEmployeeInBst(added.id()));

        data.changeEmployees(records -> { records.removeIf(employee -> employee.id().equals(added.id())); return null; });
        assertSynchronized(data);
        assertNull(data.searchEmployeeInList(added.id()));
        assertNull(data.searchEmployeeInTree(added.id()));
        assertNull(data.searchEmployeeInBst(added.id()));
    }

    @Test
    void emptyTreeInitializesAnEmptyListAndAcceptsTheFirstEmployee() throws IOException {
        Path storage = temp.resolve("empty-tree.json");
        AppData data = new AppData(new EmployeeBinaryTree(storage), requestFile(storage));
        assertTrue(data.employees().isEmpty());
        assertEquals(0, data.employeeTree().size());
        assertNull(data.searchEmployeeInBst("FIRST"));

        Employee first = employee("FIRST", "First Employee", "first.employee@example.test");
        data.insertEmployeeInTree(first);

        assertSynchronized(data);
        assertEquals(List.of(first), data.employees());
        assertEquals(first, data.searchEmployeeInBst("FIRST"));
    }

    private Path isolatedEmployeeTree() throws IOException {
        Path storage = temp.resolve("employee-tree.json");
        Files.copy(App.employeeBinaryTreeFile(), storage);
        return storage;
    }

    private Path requestFile(Path treeFile) {
        return treeFile.resolveSibling("employee-requests.json");
    }

    private static void assertSynchronized(AppData data) {
        List<Employee> linked = data.employees();
        List<Employee> tree = data.employeeTree().inorder();
        assertEquals(new HashSet<>(tree), new HashSet<>(linked));
        Set<String> ids = new HashSet<>();
        for (Employee employee : linked) assertTrue(ids.add(employee.id().toLowerCase(Locale.ROOT)));
        assertEquals(linked.size(), data.employeeTree().size());
    }

    private static Employee employee(String id, String name, String email) {
        return new Employee(id, name, "Operations", "Coordinator", email, "555-0100",
                LocalDate.parse("2025-01-01"), "ACTIVE", new BigDecimal("50000.00"));
    }
}
