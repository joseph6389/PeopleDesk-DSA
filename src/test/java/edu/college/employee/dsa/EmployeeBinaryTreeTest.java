package edu.college.employee.dsa;

import static org.junit.jupiter.api.Assertions.*;

import edu.college.employee.model.Employee;
import edu.college.employee.service.App;
import edu.college.employee.service.AppData;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmployeeBinaryTreeTest {
    @TempDir Path temp;

    @Test
    void insertsWithoutEmployeeIdOrderingAndRejectsDuplicates() throws IOException {
        EmployeeBinaryTree tree = tree();
        List<Employee> input = records("E30", "E10", "E40", "E20", "E50");
        for (Employee employee : input) tree.insert(employee);
        var root = tree.rootView();
        assertEquals("E30", root.employee().id());
        assertEquals("E10", root.left().employee().id());
        assertEquals("E40", root.right().employee().id());
        assertEquals("E20", root.left().left().employee().id());
        assertEquals("E50", root.left().right().employee().id());
        assertEquals(List.of("E20", "E10", "E50", "E30", "E40"), ids(tree.inorder()));
        assertThrows(IllegalArgumentException.class, () -> tree.insert(employee("e30")));
    }

    @Test
    void traversalsReturnActualRecordsInTheirSpecifiedOrders() throws IOException {
        EmployeeBinaryTree tree = tree();
        List<Employee> records = records("E1", "E2", "E3", "E4", "E5", "E6");
        records.forEach(employee -> assertDoesNotThrow(() -> tree.insert(employee)));

        assertEquals(List.of("E4", "E2", "E5", "E1", "E6", "E3"), ids(tree.inorder()));
        assertEquals(List.of("E1", "E2", "E4", "E5", "E3", "E6"), ids(tree.preorder()));
        assertEquals(List.of("E4", "E5", "E2", "E6", "E3", "E1"), ids(tree.postorder()));
        assertEquals(records.get(3), tree.inorder().get(0));
        assertEquals(records.get(0), tree.postorder().get(tree.size() - 1));
    }

    @Test
    void traversalsHandleEmptyAndSingleNodeTrees() throws IOException {
        EmployeeBinaryTree tree = tree();
        assertEquals(List.of(), tree.inorder());
        assertEquals(List.of(), tree.preorder());
        assertEquals(List.of(), tree.postorder());

        Employee only = employee("ONLY");
        tree.insert(only);
        assertEquals(List.of(only), tree.inorder());
        assertEquals(List.of(only), tree.preorder());
        assertEquals(List.of(only), tree.postorder());
    }

    @Test
    void searchesAndUpdatesTheMatchingRecordOnly() throws IOException {
        EmployeeBinaryTree tree = tree();
        Employee first = employee("E1"), second = employee("E2");
        tree.insert(first); tree.insert(second);
        assertEquals(first, tree.search("e1"));
        assertNull(tree.search("missing"));
        Employee changed = new Employee("E1", "Updated name", first.department(), first.designation(),
                first.email(), first.phone(), first.joiningDate(), first.status(), first.salary());
        tree.update("E1", changed);
        assertEquals(changed, tree.search("E1"));
        assertEquals(second, tree.search("E2"));
        assertThrows(IllegalArgumentException.class, () -> tree.update("E1", employee("E2")));
        assertThrows(IllegalArgumentException.class, () -> tree.update("missing", employee("E3")));
    }

    @Test
    void deletesRootLeafAndInternalNodesUsingDeepestRightmostReplacement() throws IOException {
        EmployeeBinaryTree tree = tree();
        for (Employee employee : records("E1", "E2", "E3", "E4", "E5", "E6")) tree.insert(employee);
        tree.delete("E1");
        assertEquals("E6", tree.rootView().employee().id());
        assertEquals(List.of("E4", "E2", "E5", "E6", "E3"), ids(tree.inorder()));
        tree.delete("E4");
        assertNull(tree.search("E4"));
        tree.delete("E5");
        assertNull(tree.search("E5"));
        while (tree.size() > 1) tree.delete(tree.preorder().get(tree.size() - 1).id());
        String lastId = tree.rootView().employee().id();
        tree.delete(lastId);
        assertEquals(0, tree.size());
        assertNull(tree.rootView());
        assertThrows(IllegalArgumentException.class, () -> tree.delete(lastId));
    }

    @Test
    void deletesANonRootInternalNodeByReplacingItWithTheDeepestRightmostNode() throws IOException {
        EmployeeBinaryTree tree = tree();
        for (Employee employee : records("E1", "E2", "E3", "E4", "E5", "E6")) tree.insert(employee);
        tree.delete("E2");
        assertNull(tree.search("E2"));
        assertEquals("E6", tree.rootView().left().employee().id());
        assertEquals("E4", tree.rootView().left().left().employee().id());
        assertEquals("E5", tree.rootView().left().right().employee().id());
        assertEquals(5, tree.size());
    }

    @Test
    void savesFullRecordsAndRestoresTheExactTreeAfterRestart() throws IOException {
        Path file = temp.resolve("employee-tree.json");
        EmployeeBinaryTree firstRun = new EmployeeBinaryTree(file);
        for (Employee employee : records("E30", "E10", "E40", "E20", "E50")) firstRun.insert(employee);
        var expectedRoot = firstRun.rootView();
        EmployeeBinaryTree restarted = new EmployeeBinaryTree(file);
        assertEquals(5, restarted.size());
        assertEquals(expectedRoot, restarted.rootView());
        assertEquals(firstRun.inorder(), restarted.inorder());
        String json = Files.readString(file);
        assertTrue(json.contains("\"left\""));
        assertTrue(json.contains("\"right\""));
        assertTrue(json.contains("\"email\""));
        assertTrue(json.contains("\"salary\""));
    }

    @Test
    void applicationResolvesOneTreeFileUnderTheProjectRoot() throws Exception {
        Path classes = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toAbsolutePath().normalize();
        Path projectRoot = classes.getParent().getParent();
        String originalWorkingDirectory = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", temp.toString());
            assertEquals(projectRoot.resolve("data").resolve("employee-tree.json"), App.employeeBinaryTreeFile());
            assertTrue(App.employeeBinaryTreeFile().isAbsolute());
        } finally {
            System.setProperty("user.dir", originalWorkingDirectory);
        }
    }

    @Test
    void insertUpdateAndDeleteEachPersistTheCurrentTreeStructure() throws IOException {
        Path file = temp.resolve("created-data").resolve("employee-tree.json");
        EmployeeBinaryTree tree = new EmployeeBinaryTree(file);
        assertFalse(Files.exists(file));
        tree.insert(employee("E1"));
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.isDirectory(file.getParent()));
        tree.insert(employee("E2"));
        tree.update("E1", employee("E1", "Updated Employee"));
        EmployeeBinaryTree afterUpdate = new EmployeeBinaryTree(file);
        assertEquals("Updated Employee", afterUpdate.search("E1").name());
        assertEquals("E2", afterUpdate.rootView().left().employee().id());
        afterUpdate.delete("E2");
        EmployeeBinaryTree afterDelete = new EmployeeBinaryTree(file);
        assertEquals(1, afterDelete.size());
        assertEquals("E1", afterDelete.rootView().employee().id());
        assertNull(afterDelete.rootView().left());
    }

    @Test
    void missingAndEmptyFilesStartAsEmptyTrees() throws IOException {
        Path missing = temp.resolve("missing.json");
        assertEquals(0, new EmployeeBinaryTree(missing).size());
        Path empty = temp.resolve("empty.json");
        Files.createFile(empty);
        assertEquals(0, new EmployeeBinaryTree(empty).size());
    }

    @Test
    void corruptedJsonIsPreservedAndTreeCanRecoverSafely() throws IOException {
        Path file = temp.resolve("employee-tree.json");
        Files.writeString(file, "{ this is broken json");
        EmployeeBinaryTree recovered = new EmployeeBinaryTree(file);
        assertEquals(0, recovered.size());
        assertTrue(recovered.loadWarning().contains("original file was preserved"));
        try (var files = Files.list(temp)) {
            Path backup = files.filter(path -> path.getFileName().toString().contains(".corrupt-")).findFirst().orElseThrow();
            assertEquals("{ this is broken json", Files.readString(backup));
        }
    }

    @Test
    void mainEmployeeRepositoryKeepsItsLinkedListAndBinaryTreeSynchronized() throws IOException {
        EmployeeBinaryTree tree = tree();
        AppData repository = new AppData(tree, temp.resolve("employee-requests.json"));
        Employee inserted = employee("SHARED001");

        repository.insertEmployeeInTree(inserted);
        assertEquals(List.of(inserted), repository.employees());

        Employee updated = employee("SHARED001", "Shared Updated");
        repository.updateEmployeeInTree("SHARED001", updated);
        assertEquals(updated, tree.search("SHARED001"));
        assertEquals(updated, new EmployeeBinaryTree(temp.resolve("employee-tree.json")).search("SHARED001"));

        repository.deleteEmployeeFromTree("SHARED001");
        assertEquals(0, tree.size());
        assertEquals(0, new EmployeeBinaryTree(temp.resolve("employee-tree.json")).size());
    }

    private EmployeeBinaryTree tree() throws IOException { return new EmployeeBinaryTree(temp.resolve("employee-tree.json")); }
    private static List<String> ids(List<Employee> employees) { return employees.stream().map(Employee::id).toList(); }
    private static List<Employee> records(String... ids) { return java.util.Arrays.stream(ids).map(EmployeeBinaryTreeTest::employee).toList(); }
    private static Employee employee(String id) { return employee(id, "Employee " + id); }
    private static Employee employee(String id, String name) {
        return new Employee(id, name, "Finance", "Analyst", id.toLowerCase() + "@example.test", "555-0100",
                LocalDate.parse("2024-01-01"), "ACTIVE", new BigDecimal("50000.00"));
    }
}

