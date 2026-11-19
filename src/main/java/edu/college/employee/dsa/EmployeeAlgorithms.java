package edu.college.employee.dsa;

import edu.college.employee.model.Employee;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Employee lookup and stable merge sort used by the employee screens. */
public final class EmployeeAlgorithms {
    private EmployeeAlgorithms() {}

    /** One-pass search for the employee list, including the status filter. */
    public static List<Employee> linearSearch(List<Employee> employees, String query, String status) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String requiredStatus = status == null ? "" : status.trim();
        java.util.ArrayList<Employee> matches = new java.util.ArrayList<>();
        for (Employee employee : employees) {
            if (!requiredStatus.isEmpty() && !employee.status().equalsIgnoreCase(requiredStatus)) continue;
            if (needle.isEmpty()
                    || employee.id().toLowerCase(Locale.ROOT).contains(needle)
                    || employee.name().toLowerCase(Locale.ROOT).contains(needle)
                    || employee.department().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(employee);
            }
        }
        return List.copyOf(matches);
    }

    /** Exact Employee ID lookup using iterative binary search over a Merge Sort copy. */
    public static List<Employee> binarySearch(List<Employee> employees, String employeeId, String status) {
        if (employeeId == null || employeeId.isBlank()) return List.of();
        String needle = employeeId.strip();
        List<Employee> candidates = new java.util.ArrayList<>();
        String requiredStatus = status == null ? "" : status.trim();
        for (Employee employee : employees) {
            if (requiredStatus.isEmpty() || employee.status().equalsIgnoreCase(requiredStatus)) candidates.add(employee);
        }
        List<Employee> sorted = mergeSort(candidates, "id", true);
        int low = 0, high = sorted.size() - 1;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            int comparison = sorted.get(middle).id().compareToIgnoreCase(needle);
            if (comparison == 0) return List.of(sorted.get(middle));
            if (comparison < 0) low = middle + 1;
            else high = middle - 1;
        }
        return List.of();
    }

    /** Stable merge sort for the employee table's supported fields and directions. */
    public static List<Employee> mergeSort(List<Employee> input, String field, boolean ascending) {
        if (field == null || field.isBlank()) throw new IllegalArgumentException("Choose a supported employee sort field.");
        Comparator<Employee> comparator = switch (field.toLowerCase(Locale.ROOT)) {
            case "id" -> Comparator.comparing(Employee::id, String.CASE_INSENSITIVE_ORDER);
            case "name" -> Comparator.comparing(Employee::name, String.CASE_INSENSITIVE_ORDER);
            case "salary" -> Comparator.comparing(Employee::salary);
            case "joiningdate" -> Comparator.comparing(Employee::joiningDate);
            default -> throw new IllegalArgumentException("Unsupported employee sort field.");
        };
        if (!ascending) comparator = comparator.reversed();

        Employee[] values = input.toArray(Employee[]::new);
        Employee[] temporary = new Employee[values.length];
        sort(values, temporary, 0, values.length, comparator);
        return List.copyOf(Arrays.asList(values));
    }

    private static void sort(Employee[] values, Employee[] temporary, int start, int end,
                             Comparator<Employee> comparator) {
        if (end - start < 2) return;
        int middle = start + (end - start) / 2;
        sort(values, temporary, start, middle, comparator);
        sort(values, temporary, middle, end, comparator);

        int left = start;
        int right = middle;
        int next = start;
        while (left < middle && right < end) {
            if (comparator.compare(values[left], values[right]) <= 0) temporary[next++] = values[left++];
            else temporary[next++] = values[right++];
        }
        while (left < middle) temporary[next++] = values[left++];
        while (right < end) temporary[next++] = values[right++];
        System.arraycopy(temporary, start, values, start, end - start);
    }
}
