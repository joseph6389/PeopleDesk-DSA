package edu.college.employee.dsa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.college.employee.model.Employee;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

/** Independent ordinary binary tree of employees, persisted with its exact left/right links. */
public final class EmployeeBinaryTree {
    public record NodeView(Employee employee, NodeView left, NodeView right) {}

    private static final class Node {
        private Employee employee;
        private Node left;
        private Node right;

        private Node(Employee employee) { this.employee = employee; }
    }

    private record Pending(Node node, Node parent) {}

    private final Path file;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private Node root;
    private int size;
    private String loadWarning = "";

    public EmployeeBinaryTree(Path file) throws IOException {
        this.file = file.toAbsolutePath().normalize();
        load();
    }

    public synchronized int size() { return size; }
    /** Returns actual employee records in recursive Left → Root → Right order. */
    public synchronized List<Employee> inorder() {
        List<Employee> out = new ArrayList<>();
        inorder(root, out);
        return List.copyOf(out);
    }

    /** Returns actual employee records in recursive Root → Left → Right order. */
    public synchronized List<Employee> preorder() {
        List<Employee> out = new ArrayList<>();
        preorder(root, out);
        return List.copyOf(out);
    }

    /** Returns actual employee records in recursive Left → Right → Root order. */
    public synchronized List<Employee> postorder() {
        List<Employee> out = new ArrayList<>();
        postorder(root, out);
        return List.copyOf(out);
    }

    /** Replaces the tree from employee records using the usual first-open child insertion. */
    public synchronized void replaceEmployees(List<Employee> employees) throws IOException {
        Node next = null;
        Set<String> ids = new HashSet<>();
        for (Employee employee : employees) {
            requireEmployee(employee);
            if (!ids.add(normalizedId(employee.id()))) throw new IllegalArgumentException("Employee ID already exists in this tree.");
            Node inserted = new Node(employee);
            if (next == null) next = inserted;
            else {
                Queue<Node> queue = new ArrayDeque<>();
                queue.add(next);
                while (!queue.isEmpty()) {
                    Node parent = queue.remove();
                    if (parent.left == null) { parent.left = inserted; break; }
                    queue.add(parent.left);
                    if (parent.right == null) { parent.right = inserted; break; }
                    queue.add(parent.right);
                }
            }
        }
        save(next);
        root = next;
        size = ids.size();
    }
    public synchronized NodeView rootView() { return view(root); }
    public synchronized String loadWarning() { return loadWarning; }
    /** Inserts at the first vacant child position, scanning from the root and checking left before right. */
    public synchronized Employee insert(Employee employee) throws IOException {
        requireEmployee(employee);
        if (find(root, employee.id()) != null) throw new IllegalArgumentException("Employee ID already exists in this tree.");
        Node next = copy(root);
        Node inserted = new Node(employee);
        if (next == null) next = inserted;
        else {
            Queue<Node> queue = new ArrayDeque<>();
            queue.add(next);
            while (!queue.isEmpty()) {
                Node parent = queue.remove();
                if (parent.left == null) { parent.left = inserted; break; }
                queue.add(parent.left);
                if (parent.right == null) { parent.right = inserted; break; }
                queue.add(parent.right);
            }
        }
        save(next);
        root = next;
        size++;
        return employee;
    }

    /** Searches by ID using a breadth-first traversal. */
    public synchronized Employee search(String id) { Node found = find(root, id); return found == null ? null : found.employee; }

    /** Updates the matching node without changing its position in the ordinary tree. */
    public synchronized Employee update(String id, Employee updated) throws IOException {
        requireEmployee(updated);
        Node original = find(root, id);
        if (original == null) throw new IllegalArgumentException("Employee ID was not found in this tree.");
        Node duplicate = find(root, updated.id());
        if (duplicate != null && duplicate != original) throw new IllegalArgumentException("Employee ID already exists in this tree.");
        Node next = copy(root);
        Node target = find(next, id);
        target.employee = updated;
        save(next);
        root = next;
        return updated;
    }

    /** Deletes by replacing the target with the deepest, rightmost employee node. */
    public synchronized Employee delete(String id) throws IOException {
        if (root == null) throw new IllegalArgumentException("Employee ID was not found in this tree.");
        Node next = copy(root);
        Queue<Pending> queue = new ArrayDeque<>();
        queue.add(new Pending(next, null));
        Pending target = null, deepest = null;
        while (!queue.isEmpty()) {
            Pending entry = queue.remove();
            if (equalsId(entry.node.employee.id(), id)) target = entry;
            deepest = entry;
            if (entry.node.left != null) queue.add(new Pending(entry.node.left, entry.node));
            if (entry.node.right != null) queue.add(new Pending(entry.node.right, entry.node));
        }
        if (target == null) throw new IllegalArgumentException("Employee ID was not found in this tree.");
        Employee removed = target.node.employee;
        if (deepest.node == target.node) {
            if (deepest.parent == null) next = null;
            else if (deepest.parent.right == deepest.node) deepest.parent.right = null;
            else deepest.parent.left = null;
        } else {
            target.node.employee = deepest.node.employee;
            if (deepest.parent.right == deepest.node) deepest.parent.right = null;
            else deepest.parent.left = null;
        }
        save(next);
        root = next;
        size--;
        return removed;
    }

    private void load() throws IOException {
        if (!Files.exists(file) || Files.size(file) == 0) return;
        NodeView saved;
        try {
            saved = json.readValue(file.toFile(), NodeView.class);
            Set<String> ids = new HashSet<>();
            root = fromView(saved, ids);
            size = ids.size();
        } catch (JsonProcessingException | IllegalArgumentException error) {
            recoverCorruptFile(error);
        }
    }

    private void recoverCorruptFile(Exception error) throws IOException {
        Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis());
        Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
        root = null;
        size = 0;
        loadWarning = "Saved employee tree JSON was invalid and could not be loaded. The original file was preserved as "
                + backup.getFileName() + ". Start with an empty tree or restore that backup. Details: " + error.getMessage();
    }

    private Node fromView(NodeView view, Set<String> ids) {
        if (view == null) return null;
        requireEmployee(view.employee());
        if (!ids.add(normalizedId(view.employee().id()))) throw new IllegalArgumentException("Saved tree contains duplicate Employee IDs.");
        Node node = new Node(view.employee());
        node.left = fromView(view.left(), ids);
        node.right = fromView(view.right(), ids);
        return node;
    }

    private void save(Node tree) throws IOException {
        Path parent = file.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "employee-tree-", ".tmp");
        try {
            json.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), view(tree));
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            loadWarning = "";
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Node find(Node node, String id) {
        if (node == null || id == null || id.isBlank()) return null;
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(node);
        while (!queue.isEmpty()) {
            Node current = queue.remove();
            if (equalsId(current.employee.id(), id)) return current;
            if (current.left != null) queue.add(current.left);
            if (current.right != null) queue.add(current.right);
        }
        return null;
    }

    private static Node copy(Node node) {
        if (node == null) return null;
        Node duplicate = new Node(node.employee);
        duplicate.left = copy(node.left);
        duplicate.right = copy(node.right);
        return duplicate;
    }

    private static NodeView view(Node node) {
        return node == null ? null : new NodeView(node.employee, view(node.left), view(node.right));
    }

    private static void inorder(Node node, List<Employee> out) {
        if (node == null) return;
        inorder(node.left, out);
        out.add(node.employee);
        inorder(node.right, out);
    }

    private static void preorder(Node node, List<Employee> out) {
        if (node == null) return;
        out.add(node.employee);
        preorder(node.left, out);
        preorder(node.right, out);
    }

    private static void postorder(Node node, List<Employee> out) {
        if (node == null) return;
        postorder(node.left, out);
        postorder(node.right, out);
        out.add(node.employee);
    }

    private static void requireEmployee(Employee employee) {
        if (employee == null) throw new IllegalArgumentException("Complete employee details are required.");
    }

    private static boolean equalsId(String left, String right) {
        return left != null && right != null && left.strip().equalsIgnoreCase(right.strip());
    }

    private static String normalizedId(String id) { return id.strip().toLowerCase(Locale.ROOT); }
}
