package edu.college.employee.dsa;

import edu.college.employee.model.Employee;
import java.util.List;
import java.util.Locale;

/** In-memory Employee ID Binary Search Tree used only by the Employees search option. */
public final class EmployeeSearchBst {
    private static final class Node {
        private final Employee employee;
        private Node left;
        private Node right;
        private Node(Employee employee) { this.employee = employee; }
    }

    private Node root;
    private int size;

    public synchronized int size() { return size; }

    public synchronized boolean insert(Employee employee) {
        if (employee == null || employee.id() == null || employee.id().isBlank())
            throw new IllegalArgumentException("Employee ID is required for BST insertion.");
        if (root == null) {
            root = new Node(employee);
            size = 1;
            return true;
        }
        Node current = root;
        while (true) {
            int comparison = compare(employee.id(), current.employee.id());
            if (comparison == 0) return false;
            if (comparison < 0) {
                if (current.left == null) {
                    current.left = new Node(employee);
                    size++;
                    return true;
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    current.right = new Node(employee);
                    size++;
                    return true;
                }
                current = current.right;
            }
        }
    }

    public synchronized Employee search(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) return null;
        Node current = root;
        while (current != null) {
            int comparison = compare(employeeId.strip(), current.employee.id());
            if (comparison == 0) return current.employee;
            current = comparison < 0 ? current.left : current.right;
        }
        return null;
    }

    /** Rebuilds the search index from the current authoritative employee records. */
    public synchronized void rebuild(List<Employee> employees) {
        EmployeeSearchBst replacement = new EmployeeSearchBst();
        for (Employee employee : employees) {
            if (!replacement.insert(employee)) throw new IllegalArgumentException("Employee ID already exists in the search BST.");
        }
        root = replacement.root;
        size = replacement.size;
    }

    private static int compare(String first, String second) {
        return first.toLowerCase(Locale.ROOT).compareTo(second.toLowerCase(Locale.ROOT));
    }
}
