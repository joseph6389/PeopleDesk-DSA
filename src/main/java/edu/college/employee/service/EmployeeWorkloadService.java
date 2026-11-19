package edu.college.employee.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.college.employee.dsa.GreedyWorkloadAllocator;
import edu.college.employee.model.Employee;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Workload metrics and allocation history backed by separate JSON files. */
public final class EmployeeWorkloadService {
    public record WorkloadRecord(String employeeId, int currentWorkloadUnits, int assignedTaskCount, Instant updatedAt) {}
    public record WorkUnit(String taskId, String taskName, String description, String difficulty, int weight,
                          Instant createdAt) {}
    public record EmployeeWorkload(String employeeId, String employeeName, String department, String designation,
                                   String status, WorkloadRecord workload, boolean eligible) {}
    public record WorkloadSnapshot(String employeeId, int currentWorkloadUnits, int assignedTaskCount) {}
    public record AllocationItem(String taskId, String taskName, String difficulty, int weight,
                                 String assignedEmployeeId, String assignedEmployeeName, String assignedDepartment,
                                 int previousWorkload, int updatedWorkload,
                                 List<GreedyWorkloadAllocator.EligibleWorkload> eligibleWorkloads,
                                 boolean tieBreakApplied, String reason) {
        public AllocationItem {
            eligibleWorkloads = eligibleWorkloads == null ? List.of() : List.copyOf(eligibleWorkloads);
        }
    }
    public record AllocationRun(String runId, Instant allocatedAt, int totalWorkUnits, long totalWorkloadWeight,
                                int totalAllocatedTasks, int totalUnallocatedTasks, int eligibleEmployeeCount,
                                List<AllocationItem> tasks, List<WorkloadSnapshot> before,
                                List<WorkloadSnapshot> after) {}
    public record SimulationPlan(List<AllocationItem> tasks, int eligibleEmployeeCount,
                                 long totalWorkloadWeight, List<WorkloadSnapshot> before,
                                 List<WorkloadSnapshot> after) {}
    public record View(List<EmployeeWorkload> employees, List<WorkUnit> pendingTasks,
                       AllocationRun latestRun, List<AllocationRun> history, String message) {}

    private static final int MAX_PENDING_TASKS = 200;
    private static final int MAX_TASK_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private final Supplier<List<Employee>> employeeSource;
    private final Path workloadFile;
    private final Path historyFile;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final GreedyWorkloadAllocator allocator = new GreedyWorkloadAllocator();
    private final Map<String, WorkloadRecord> workloads = new LinkedHashMap<>();
    private final List<WorkUnit> pendingTasks = new ArrayList<>();
    private final List<AllocationRun> history = new ArrayList<>();

    public EmployeeWorkloadService(Supplier<List<Employee>> employeeSource, Path dataDirectory) throws IOException {
        this.employeeSource = Objects.requireNonNull(employeeSource);
        Path directory = dataDirectory.toAbsolutePath().normalize();
        this.workloadFile = directory.resolve("employee-workload.json");
        this.historyFile = directory.resolve("workload-allocation-history.json");
        loadFiles();
        synchronizeEmployeeWorkloads();
    }

    public Path workloadFile() { return workloadFile; }
    public Path historyFile() { return historyFile; }

    /** Adds zeroed entries for any employees inserted since the workload view was last opened. */
    public synchronized void synchronizeEmployees() throws IOException { synchronizeEmployeeWorkloads(); }

    public synchronized View view() throws IOException { return view("Workload records loaded."); }

    public synchronized View addTask(String name, String description, String difficulty) throws IOException {
        synchronizeEmployeeWorkloads();
        String taskName = name == null ? "" : name.strip();
        String taskDescription = description == null ? "" : description.strip();
        if (taskName.isBlank() || taskName.length() > MAX_TASK_NAME_LENGTH)
            throw new IllegalArgumentException("Task name is required and must be at most 120 characters.");
        if (taskDescription.length() > MAX_DESCRIPTION_LENGTH)
            throw new IllegalArgumentException("Task description must be at most 1000 characters.");
        if (pendingTasks.size() >= MAX_PENDING_TASKS) throw new IllegalArgumentException("A maximum of 200 pending tasks is allowed.");
        String normalizedDifficulty = normalizeDifficulty(difficulty);
        int weight = weightFor(normalizedDifficulty);
        pendingTasks.add(new WorkUnit("TASK-" + UUID.randomUUID(), taskName, taskDescription,
                normalizedDifficulty, weight, Instant.now()));
        return view("Work unit added to the pending list.");
    }

    public synchronized View removeTask(String taskId) throws IOException {
        synchronizeEmployeeWorkloads();
        boolean removed = pendingTasks.removeIf(task -> task.taskId().equals(taskId));
        if (!removed) throw new NoSuchElementException("Pending work unit was not found.");
        return view("Pending work unit removed.");
    }

    public synchronized View allocate() throws IOException {
        synchronizeEmployeeWorkloads();
        if (pendingTasks.isEmpty()) throw new IllegalArgumentException("Add at least one pending work unit before allocation.");
        List<Employee> currentEmployees = List.copyOf(employeeSource.get());
        List<Employee> eligible = currentEmployees.stream().filter(employee -> "ACTIVE".equalsIgnoreCase(employee.status())).toList();
        if (eligible.isEmpty()) throw new IllegalArgumentException("No ACTIVE employees are available; pending work units were kept.");
        List<WorkloadSnapshot> before = snapshots(eligible, workloads);
        Map<String, Integer> currentLoads = new LinkedHashMap<>();
        for (Employee employee : eligible) currentLoads.put(key(employee.id()), workloads.get(key(employee.id())).currentWorkloadUnits());

        List<GreedyWorkloadAllocator.WorkUnit> input = pendingTasks.stream()
                .map(task -> new GreedyWorkloadAllocator.WorkUnit(task.taskId(), task.taskName(), task.difficulty(), task.weight()))
                .toList();
        GreedyWorkloadAllocator.Result result = allocator.allocate(currentEmployees, currentLoads, input);
        Map<String, WorkloadRecord> nextWorkloads = new LinkedHashMap<>(workloads);
        Instant now = Instant.now();
        Map<String, Integer> updatedCounts = new LinkedHashMap<>();
        for (Employee employee : eligible) updatedCounts.put(key(employee.id()), workloads.get(key(employee.id())).assignedTaskCount());
        for (GreedyWorkloadAllocator.Assignment assignment : result.assignments()) {
            String employeeKey = key(assignment.employeeId());
            updatedCounts.put(employeeKey, Math.addExact(updatedCounts.get(employeeKey), 1));
            nextWorkloads.put(employeeKey, new WorkloadRecord(assignment.employeeId(), assignment.updatedWorkload(),
                    updatedCounts.get(employeeKey), now));
        }

        List<AllocationItem> items = allocationItems(result);
        int totalWeight = pendingTasks.stream().mapToInt(WorkUnit::weight).sum();
        AllocationRun run = new AllocationRun(UUID.randomUUID().toString(), now, pendingTasks.size(), totalWeight,
                result.assignments().size(), result.unallocated().size(), eligible.size(), List.copyOf(items), before,
                snapshots(eligible, nextWorkloads));
        List<AllocationRun> nextHistory = new ArrayList<>(history);
        nextHistory.add(0, run);
        persistAllocation(nextWorkloads, nextHistory);
        workloads.clear();
        workloads.putAll(nextWorkloads);
        history.clear();
        history.addAll(nextHistory);
        pendingTasks.clear();
        return view("Greedy allocation completed and saved.");
    }

    /** Builds an in-memory preview using the real greedy allocator without changing storage or pending work. */
    public synchronized SimulationPlan simulate() {
        if (pendingTasks.isEmpty()) throw new IllegalArgumentException("Add at least one pending work unit before simulation.");
        List<Employee> currentEmployees = List.copyOf(employeeSource.get());
        List<Employee> eligible = currentEmployees.stream()
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.status())).toList();
        Map<String, Integer> currentLoads = new LinkedHashMap<>();
        List<WorkloadSnapshot> before = new ArrayList<>();
        for (Employee employee : eligible) {
            WorkloadRecord record = workloads.get(key(employee.id()));
            int current = record == null ? 0 : record.currentWorkloadUnits();
            currentLoads.put(key(employee.id()), current);
            before.add(new WorkloadSnapshot(employee.id(), current, record == null ? 0 : record.assignedTaskCount()));
        }
        List<GreedyWorkloadAllocator.WorkUnit> input = pendingTasks.stream()
                .map(task -> new GreedyWorkloadAllocator.WorkUnit(task.taskId(), task.taskName(), task.difficulty(), task.weight()))
                .toList();
        GreedyWorkloadAllocator.Result result = allocator.allocate(currentEmployees, currentLoads, input);
        List<WorkloadSnapshot> after = eligible.stream().map(employee -> {
            int load = result.workloadAfter().getOrDefault(key(employee.id()), currentLoads.get(key(employee.id())));
            long assigned = result.assignments().stream()
                    .filter(assignment -> assignment.employeeId().equalsIgnoreCase(employee.id())).count();
            WorkloadRecord record = workloads.get(key(employee.id()));
            return new WorkloadSnapshot(employee.id(), load, (record == null ? 0 : record.assignedTaskCount()) + (int) assigned);
        }).toList();
        return new SimulationPlan(allocationItems(result), eligible.size(), pendingTasks.stream().mapToLong(WorkUnit::weight).sum(),
                List.copyOf(before), after);
    }

    private static List<AllocationItem> allocationItems(GreedyWorkloadAllocator.Result result) {
        List<AllocationItem> items = new ArrayList<>();
        for (GreedyWorkloadAllocator.Assignment assignment : result.assignments()) {
            var task = assignment.task();
            String reason = assignment.tieBreakApplied()
                    ? "Several ACTIVE employees had " + assignment.previousWorkload() + " units; "
                        + assignment.employeeId() + " was selected because its Employee ID was lowest."
                    : assignment.employeeId() + " had the lowest current workload (" + assignment.previousWorkload()
                        + " units) among " + assignment.eligibleWorkloads().size() + " eligible ACTIVE employees.";
            items.add(new AllocationItem(task.taskId(), task.taskName(), task.difficulty(), task.weight(),
                    assignment.employeeId(), assignment.employeeName(), assignment.department(),
                    assignment.previousWorkload(), assignment.updatedWorkload(), assignment.eligibleWorkloads(),
                    assignment.tieBreakApplied(), reason));
        }
        for (GreedyWorkloadAllocator.Unallocated item : result.unallocated()) {
            var task = item.task();
            items.add(new AllocationItem(task.taskId(), task.taskName(), task.difficulty(), task.weight(),
                    null, null, null, 0, 0, List.of(), false, item.reason()));
        }
        return List.copyOf(items);
    }

    public synchronized View resetWorkload(String employeeId) throws IOException {
        synchronizeEmployeeWorkloads();
        Employee employee = employeeSource.get().stream()
                .filter(record -> record.id().equalsIgnoreCase(employeeId == null ? "" : employeeId.strip()))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Employee record was not found."));
        String employeeKey = key(employee.id());
        WorkloadRecord previous = workloads.get(employeeKey);
        WorkloadRecord reset = new WorkloadRecord(employee.id(), 0, previous.assignedTaskCount(), Instant.now());
        Map<String, WorkloadRecord> next = new LinkedHashMap<>(workloads);
        next.put(employeeKey, reset);
        writeJson(workloadFile, next);
        workloads.clear();
        workloads.putAll(next);
        return view("Workload reset to zero for " + employee.id() + ".");
    }

    private View view(String message) throws IOException {
        synchronizeEmployeeWorkloads();
        List<EmployeeWorkload> employees = new ArrayList<>();
        for (Employee employee : employeeSource.get()) {
            WorkloadRecord record = workloads.get(key(employee.id()));
            employees.add(new EmployeeWorkload(employee.id(), employee.name(), employee.department(), employee.designation(),
                    employee.status(), record, "ACTIVE".equalsIgnoreCase(employee.status())));
        }
        return new View(List.copyOf(employees), List.copyOf(pendingTasks),
                history.isEmpty() ? null : history.get(0), List.copyOf(history), message);
    }

    private void synchronizeEmployeeWorkloads() throws IOException {
        boolean changed = false;
        for (Employee employee : employeeSource.get()) {
            if (employee == null || employee.id() == null || employee.id().isBlank())
                throw new IOException("Employee records contain an invalid ID; workload storage was not changed.");
            String employeeKey = key(employee.id());
            if (!workloads.containsKey(employeeKey)) {
                workloads.put(employeeKey, new WorkloadRecord(employee.id(), 0, 0, Instant.now()));
                changed = true;
            }
        }
        if (changed || !Files.exists(workloadFile)) writeJson(workloadFile, workloads);
        if (!Files.exists(historyFile)) writeJson(historyFile, history);
    }

    private void loadFiles() throws IOException {
        if (Files.exists(workloadFile)) {
            try {
                Map<String, WorkloadRecord> stored = json.readValue(workloadFile.toFile(),
                        new TypeReference<LinkedHashMap<String, WorkloadRecord>>() {});
                if (stored == null) throw new IOException("Workload JSON must contain an object.");
                Set<String> ids = new HashSet<>();
                for (Map.Entry<String, WorkloadRecord> entry : stored.entrySet()) {
                    WorkloadRecord record = entry.getValue();
                    if (record == null || record.employeeId() == null || record.employeeId().isBlank()
                            || record.currentWorkloadUnits() < 0 || record.assignedTaskCount() < 0 || record.updatedAt() == null)
                        throw new IOException("Workload JSON contains an invalid record.");
                    String id = key(record.employeeId());
                    if (!id.equals(key(entry.getKey())) || !ids.add(id)) throw new IOException("Workload JSON contains a duplicate or mismatched employee ID.");
                    workloads.put(id, record);
                }
            } catch (IOException | RuntimeException error) {
                throw new IOException("Could not load " + workloadFile + "; the file was left unchanged. " + error.getMessage(), error);
            }
        }
        if (Files.exists(historyFile)) {
            try {
                List<AllocationRun> saved = json.readValue(historyFile.toFile(), new TypeReference<List<AllocationRun>>() {});
                if (saved == null) throw new IOException("Allocation history JSON must contain an array.");
                for (AllocationRun run : saved) if (run == null || run.runId() == null || run.allocatedAt() == null || run.tasks() == null)
                    throw new IOException("Allocation history contains an invalid run.");
                history.addAll(saved);
            } catch (IOException | RuntimeException error) {
                throw new IOException("Could not load " + historyFile + "; the file was left unchanged. " + error.getMessage(), error);
            }
        }
    }

    private void persistAllocation(Map<String, WorkloadRecord> nextWorkloads, List<AllocationRun> nextHistory) throws IOException {
        Path workloadTemporary = writeTemporary(workloadFile, nextWorkloads);
        Path historyTemporary = null;
        boolean workloadReplaced = false;
        try {
            historyTemporary = writeTemporary(historyFile, nextHistory);
            replace(workloadTemporary, workloadFile);
            workloadReplaced = true;
            replace(historyTemporary, historyFile);
        } catch (IOException error) {
            if (workloadReplaced) {
                try { writeJson(workloadFile, workloads); } catch (IOException rollbackError) { error.addSuppressed(rollbackError); }
            }
            throw error;
        } finally {
            Files.deleteIfExists(workloadTemporary);
            if (historyTemporary != null) Files.deleteIfExists(historyTemporary);
        }
    }

    private void writeJson(Path target, Object value) throws IOException {
        Path temporary = writeTemporary(target, value);
        try { replace(temporary, target); }
        finally { Files.deleteIfExists(temporary); }
    }

    private Path writeTemporary(Path target, Object value) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName() + "-", ".tmp");
        try {
            json.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value);
            return temporary;
        } catch (IOException error) {
            Files.deleteIfExists(temporary);
            throw error;
        }
    }

    private static void replace(Path temporary, Path target) throws IOException {
        try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static List<WorkloadSnapshot> snapshots(List<Employee> employees, Map<String, WorkloadRecord> values) {
        return employees.stream().map(employee -> {
            WorkloadRecord record = values.get(key(employee.id()));
            return new WorkloadSnapshot(employee.id(), record.currentWorkloadUnits(), record.assignedTaskCount());
        }).toList();
    }

    private static String normalizeDifficulty(String difficulty) {
        if (difficulty == null) throw new IllegalArgumentException("Choose Easy, Medium, or Difficult difficulty.");
        return switch (difficulty.strip().toLowerCase(Locale.ROOT)) {
            case "easy" -> "Easy";
            case "medium" -> "Medium";
            case "difficult" -> "Difficult";
            default -> throw new IllegalArgumentException("Choose Easy, Medium, or Difficult difficulty.");
        };
    }

    private static int weightFor(String difficulty) {
        return switch (difficulty) { case "Easy" -> 1; case "Medium" -> 2; case "Difficult" -> 3; default -> throw new IllegalArgumentException("Invalid task difficulty."); };
    }

    private static String key(String employeeId) { return employeeId.toLowerCase(Locale.ROOT); }
}
