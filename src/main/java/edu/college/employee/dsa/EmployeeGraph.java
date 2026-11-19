package edu.college.employee.dsa;

import edu.college.employee.model.Employee;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Undirected graph stored as an adjacency list. */
public final class EmployeeGraph {
    private final Map<String, List<String>> adjacency = new LinkedHashMap<>();

    public void addVertex(String id) {
        adjacency.computeIfAbsent(Objects.requireNonNull(id), ignored -> new ArrayList<>());
    }

    public void addEdge(String first, String second) {
        if (!adjacency.containsKey(first) || !adjacency.containsKey(second)) {
            throw new IllegalArgumentException("Both employees must exist as vertices");
        }
        if (first.equals(second)) {
            throw new IllegalArgumentException("Self connections are not allowed");
        }
        if (!adjacency.get(first).contains(second)) {
            adjacency.get(first).add(second);
            adjacency.get(second).add(first);
        }
    }

    /** Returns up to five actual employee records in stable Employee ID order. */
    public static List<Employee> sampleEmployeeVertices(List<Employee> employees) {
        Objects.requireNonNull(employees, "employees");
        return employees.stream()
                .sorted(Comparator.comparing(Employee::id, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .toList();
    }

    /**
     * Builds temporary illustrative edges over actual employee IDs in stable
     * case-insensitive ID order. These sample connections are not stored in
     * PeopleDesk employee records.
     */
    public static EmployeeGraph sampleEmployeeConnections(List<Employee> employees) {
        List<Employee> vertices = Objects.requireNonNull(employees, "employees").stream()
                .sorted(Comparator.comparing(Employee::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
        EmployeeGraph graph = new EmployeeGraph();
        vertices.forEach(employee -> graph.addVertex(employee.id()));
        int[][] sampleEdges = {{0, 1}, {0, 2}, {1, 3}, {1, 4}};
        for (int[] edge : sampleEdges) {
            if (edge[1] < vertices.size()) {
                graph.addEdge(vertices.get(edge[0]).id(), vertices.get(edge[1]).id());
            }
        }
        return graph;
    }

    /** Builds the actual bipartite employee-to-department membership graph. */
    public static EmployeeGraph departmentMembershipGraph(List<Employee> employees, List<String> departments) {
        Objects.requireNonNull(employees, "employees");
        Objects.requireNonNull(departments, "departments");
        EmployeeGraph graph = new EmployeeGraph();
        employees.stream()
                .sorted(Comparator.comparing(Employee::id, String.CASE_INSENSITIVE_ORDER))
                .forEach(employee -> graph.addVertex(employee.id()));
        departments.stream().filter(Objects::nonNull).distinct().sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(graph::addVertex);
        for (Employee employee : employees) {
            String department = departments.stream()
                    .filter(candidate -> candidate.equalsIgnoreCase(employee.department()))
                    .findFirst().orElse(employee.department());
            if (!graph.adjacency.containsKey(department)) {
                throw new IllegalArgumentException("Employee department must be included as a graph vertex");
            }
            graph.addEdge(employee.id(), department);
        }
        return graph;
    }

    public List<String> vertices() {
        return List.copyOf(adjacency.keySet());
    }

    /** Returns a symmetric 0/1 matrix in the same stable order as vertices(). */
    public List<List<Integer>> adjacencyMatrix() {
        List<String> vertices = vertices();
        List<List<Integer>> matrix = new ArrayList<>(vertices.size());
        for (String row : vertices) {
            List<Integer> values = new ArrayList<>(vertices.size());
            for (String column : vertices) {
                values.add(adjacency.get(row).contains(column) ? 1 : 0);
            }
            matrix.add(List.copyOf(values));
        }
        return List.copyOf(matrix);
    }

    public Map<String, List<String>> adjacency() {
        Map<String, List<String>> snapshot = new LinkedHashMap<>();
        adjacency.forEach((id, neighbors) -> snapshot.put(id, List.copyOf(neighbors)));
        return Collections.unmodifiableMap(snapshot);
    }

    /** Returns the induced graph containing only the requested existing vertices. */
    public EmployeeGraph inducedSubgraph(Set<String> selectedIds) {
        Objects.requireNonNull(selectedIds, "selectedIds");
        EmployeeGraph selected = new EmployeeGraph();
        adjacency.keySet().stream().filter(selectedIds::contains).forEach(selected::addVertex);
        adjacency.forEach((id, neighbors) -> {
            if (!selected.adjacency.containsKey(id)) return;
            for (String neighbor : neighbors) {
                if (selected.adjacency.containsKey(neighbor)
                        && id.compareTo(neighbor) < 0) selected.addEdge(id, neighbor);
            }
        });
        return selected;
    }

    /** Breadth-first traversal of the connected component containing start. */
    public List<String> bfs(String start) {
        if (!adjacency.containsKey(start)) return List.of();
        Set<String> visited = new HashSet<>();
        LinkedQueue<String> queue = new LinkedQueue<>();
        List<String> order = new ArrayList<>();
        visited.add(start);
        queue.enqueue(start);
        while (!queue.isEmpty()) {
            String current = queue.dequeue();
            order.add(current);
            for (String neighbor : adjacency.get(current)) {
                if (visited.add(neighbor)) queue.enqueue(neighbor);
            }
        }
        return List.copyOf(order);
    }
}
