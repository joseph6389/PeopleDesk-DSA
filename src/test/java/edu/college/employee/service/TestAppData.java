package edu.college.employee.service;

import edu.college.employee.dsa.EmployeeBinaryTree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Isolated application repository backed by a copy of the real migrated employee tree. */
public final class TestAppData {
    private TestAppData() {}

    public static AppData create() {
        try {
            Path projectFile = App.employeeBinaryTreeFile();
            Path directory = Files.createTempDirectory("peopledesk-test-");
            Path file = directory.resolve("employee-tree.json");
            Files.copy(projectFile, file);
            return new AppData(new EmployeeBinaryTree(file), directory.resolve("employee-requests.json"));
        } catch (IOException error) {
            throw new IllegalStateException("Could not prepare isolated employee-tree test data.", error);
        }
    }
}
