package edu.college.employee.service;

import edu.college.employee.model.Activity;
import edu.college.employee.dsa.EmployeeBinaryTree;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/** Shared in-memory application state for this server process. */
public final class App {
    private static volatile AppData data;
    private static volatile EmployeeBinaryTree employeeBinaryTree;
    private static volatile EmployeeWorkloadService employeeWorkloadService;

    private App() {}

    public static AppData repo() {
        AppData current = data;
        if (current == null) {
            synchronized (App.class) {
                current = data;
                if (current == null) {
                    try {
                        data = current = new AppData(employeeBinaryTree(), employeeRequestFile());
                    } catch (IOException error) {
                        throw new IllegalStateException("Could not load persistent employee records.", error);
                    }
                }
            }
        }
        return current;
    }

    public static EmployeeBinaryTree employeeBinaryTree() throws IOException {
        EmployeeBinaryTree current = employeeBinaryTree;
        if (current == null) {
            synchronized (App.class) {
                current = employeeBinaryTree;
                if (current == null) {
                    employeeBinaryTree = current = new EmployeeBinaryTree(employeeBinaryTreeFile());
                }
            }
        }
        return current;
    }

    public static EmployeeWorkloadService employeeWorkloadService() throws IOException {
        EmployeeWorkloadService current = employeeWorkloadService;
        if (current == null) {
            synchronized (App.class) {
                current = employeeWorkloadService;
                if (current == null) {
                    Path dataDirectory = employeeBinaryTreeFile().getParent();
                    employeeWorkloadService = current = new EmployeeWorkloadService(() -> repo().employees(), dataDirectory);
                }
            }
        }
        return current;
    }

    /** Best-effort synchronization after employee CRUD, without blocking employee operations on workload storage. */
    public static void synchronizeWorkloadsIfInitialized() {
        EmployeeWorkloadService current = employeeWorkloadService;
        if (current == null) return;
        try {
            current.synchronizeEmployees();
        } catch (IOException error) {
            System.err.println("Employee workload records could not be synchronized: " + error.getMessage());
        }
    }

    /** Resolves the one project-root storage file independently of the server's working directory. */
    public static Path employeeBinaryTreeFile() throws IOException {
        Path location;
        try {
            location = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath().normalize();
        } catch (URISyntaxException | NullPointerException error) {
            throw new IOException("Could not locate the PeopleDesk project to resolve employee-tree.json.", error);
        }
        Path directory = Files.isDirectory(location) ? location : location.getParent();
        for (Path candidate = directory; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("run.ps1"))) {
                return candidate.resolve("data").resolve("employee-tree.json").toAbsolutePath().normalize();
            }
        }
        throw new IOException("Could not locate the PeopleDesk project root (run.ps1) for employee-tree.json.");
    }

    /** Resolves the durable employee request history beside the employee tree storage. */
    public static Path employeeRequestFile() throws IOException {
        return employeeBinaryTreeFile().resolveSibling("employee-requests.json");
    }

    public static void log(AppData repository, String actor, String action, String target) {
        repository.recordActivity(new Activity(UUID.randomUUID().toString(), Instant.now(), actor, action, target));
    }
}
