package edu.college.employee.dsa;

import edu.college.employee.model.Employee;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Assigns each task to the currently least-loaded active employee. */
public final class GreedyWorkloadAllocator {
    public record WorkUnit(String taskId, String taskName, String difficulty, int weight) {
        public WorkUnit {
            if (taskId == null || taskId.isBlank() || taskName == null || taskName.isBlank())
                throw new IllegalArgumentException("Task ID and name are required.");
            int expectedWeight = switch (difficulty == null ? "" : difficulty.toLowerCase(Locale.ROOT)) {
                case "easy" -> 1;
                case "medium" -> 2;
                case "difficult" -> 3;
                default -> throw new IllegalArgumentException("Difficulty must be Easy, Medium, or Difficult.");
            };
            if (weight != expectedWeight) throw new IllegalArgumentException("Task workload weight does not match its difficulty.");
        }
    }

    public record EligibleWorkload(String employeeId, String employeeName, String department, int workload) {}
    public record Assignment(WorkUnit task, String employeeId, String employeeName, String department,
                             int previousWorkload, int updatedWorkload,
                             List<EligibleWorkload> eligibleWorkloads, boolean tieBreakApplied) {}
    public record Unallocated(WorkUnit task, String reason) {}
    public record Result(List<Assignment> assignments, List<Unallocated> unallocated,
                         Map<String, Integer> workloadAfter) {}

    /**
     * Greedy scan: for each task, inspect every active employee using the latest in-run
     * workload, breaking ties by Employee ID. Time O(T × E); extra space O(E + T).
     */
    public Result allocate(List<Employee> employees, Map<String, Integer> currentWorkloads,
                           List<WorkUnit> tasks) {
        List<Employee> eligible = employees.stream()
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.status()))
                .toList();
        Map<String, Integer> loads = new LinkedHashMap<>();
        for (Employee employee : eligible) {
            int workload = currentWorkloads.getOrDefault(key(employee.id()), 0);
            if (workload < 0) throw new IllegalArgumentException("Employee workload cannot be negative.");
            loads.put(key(employee.id()), workload);
        }

        List<Assignment> assignments = new ArrayList<>();
        List<Unallocated> unallocated = new ArrayList<>();
        for (WorkUnit task : tasks) {
            if (eligible.isEmpty()) {
                unallocated.add(new Unallocated(task, "No ACTIVE employees are available."));
                continue;
            }
            List<EligibleWorkload> decisionState = eligible.stream()
                    .map(employee -> new EligibleWorkload(employee.id(), employee.name(), employee.department(),
                            loads.get(key(employee.id()))))
                    .toList();
            Employee selected = null;
            int lowest = Integer.MAX_VALUE;
            int lowestCount = 0;
            for (Employee candidate : eligible) {
                int load = loads.get(key(candidate.id()));
                if (load < lowest) {
                    selected = candidate;
                    lowest = load;
                    lowestCount = 1;
                } else if (load == lowest) {
                    lowestCount++;
                    if (selected == null || compareEmployeeIds(candidate.id(), selected.id()) < 0) selected = candidate;
                }
            }
            int updated;
            try {
                updated = Math.addExact(lowest, task.weight());
            } catch (ArithmeticException error) {
                throw new IllegalArgumentException("Employee workload exceeds the supported limit.", error);
            }
            loads.put(key(selected.id()), updated);
            assignments.add(new Assignment(task, selected.id(), selected.name(), selected.department(), lowest,
                    updated, decisionState, lowestCount > 1));
        }
        return new Result(List.copyOf(assignments), List.copyOf(unallocated), Map.copyOf(loads));
    }

    private static int compareEmployeeIds(String left, String right) {
        int result = left.compareToIgnoreCase(right);
        return result != 0 ? result : left.compareTo(right);
    }

    private static String key(String employeeId) { return employeeId.toLowerCase(Locale.ROOT); }
}
