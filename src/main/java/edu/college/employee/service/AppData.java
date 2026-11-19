package edu.college.employee.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.college.employee.dsa.EmployeeBinaryTree;
import edu.college.employee.dsa.EmployeeSearchBst;
import edu.college.employee.dsa.LinkedQueue;
import edu.college.employee.dsa.LinkedStack;
import edu.college.employee.dsa.SinglyLinkedList;
import edu.college.employee.model.Activity;
import edu.college.employee.model.Employee;
import edu.college.employee.model.EmployeeRequest;
import edu.college.employee.model.RegistrationRequest;
import edu.college.employee.model.User;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.time.Instant;

/** Shared application data; employee records are backed by the persistent binary tree. */
public final class AppData {
    private static final String ADMIN_HASH = "$2a$12$JDYvLc4Cp0efB1KXQXKChuGmAPYaxOw.UNzAhHMBywaX3S51t/3dq";
    private static final String HR_HASH = "$2a$12$gQA1QUsATmS1fhdupM2RWeVT2orkYOwS87V8s6MK3DKYQJlzgluUK";
    private static final String EMPLOYEE_HASH = "$2a$12$9c5Kfxgqz3J9zuGCAg7zbO3fr8T8Hp9DEa9.5x5J3UnfUCh34vxj6";
    private final EmployeeBinaryTree employeeTree;
    private final EmployeeSearchBst employeeSearchBst = new EmployeeSearchBst();
    private SinglyLinkedList<Employee> employeeList;
    private final List<User> users = new ArrayList<>();
    private final List<String> departments = new ArrayList<>();
    private final List<RegistrationRequest> registrationRequests = new ArrayList<>();
    private final List<EmployeeRequest> requests;
    private final LinkedQueue<EmployeeRequest> pendingLeaveQueue = new LinkedQueue<>();
    private final Path requestFile;
    private final ObjectMapper requestJson = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final LinkedStack<Activity> activityStack = new LinkedStack<>();

    public AppData(EmployeeBinaryTree employeeTree, Path requestFile) throws IOException {
        this.employeeTree = Objects.requireNonNull(employeeTree);
        this.requestFile = Objects.requireNonNull(requestFile).toAbsolutePath().normalize();
        this.employeeList = linkedList(employeeTree.inorder());
        this.employeeSearchBst.rebuild(employeeList.traverse());
        this.requests = loadRequests();
        rebuildPendingLeaveQueue();
        verifyEmployeeStructures(employeeList.traverse(), employeeTree.inorder());
        Set<String> initialDepartments = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        employees().stream().map(Employee::department).forEach(initialDepartments::add);
        departments.addAll(initialDepartments);
        addDemoAccount("admin", ADMIN_HASH, "ADMIN", null);
        addDemoAccount("hr-admin", HR_HASH, "HR", null);
        addDemoAccount("employee", EMPLOYEE_HASH, "EMPLOYEE", "EMP001");
    }

    /** Adds a built-in demo user once; credentials are stored only as precomputed BCrypt hashes. */
    private void addDemoAccount(String username, String passwordHash, String role, String employeeId) {
        if (users.stream().anyMatch(user -> user.username().equalsIgnoreCase(username))) return;
        if ("EMPLOYEE".equals(role) && users.stream().anyMatch(user -> user.role().equals("EMPLOYEE")
                && user.employeeId() != null && employeeId.equalsIgnoreCase(user.employeeId()))) return;
        users.add(new User(username, passwordHash, role, employeeId));
    }

    /** Returns the in-memory employee sequence by traversing the synchronized singly linked list. */
    public synchronized List<Employee> employees() { return employeeList.traverse(); }

    /** Sequential Employee ID search through the live in-memory linked list. */
    public synchronized Employee searchEmployeeInList(String id) {
        if (id == null || id.isBlank()) return null;
        String key = id.strip();
        return employeeList.search(employee -> employee.id().equalsIgnoreCase(key));
    }

    /** Employee ID search through the canonical ordinary Binary Tree. */
    public synchronized Employee searchEmployeeInTree(String id) { return employeeTree.search(id); }

    /** Exact Employee ID lookup in the in-memory employee search BST. */
    public synchronized Employee searchEmployeeInBst(String id) { return employeeSearchBst.search(id); }

    public synchronized EmployeeBinaryTree employeeTree() { return employeeTree; }

    public synchronized <R> R changeEmployees(Function<List<Employee>, R> action) {
        List<Employee> changed = new ArrayList<>(employees());
        R result = action.apply(changed);
        validateEmployees(changed);
        SinglyLinkedList<Employee> nextList = linkedList(changed);
        try {
            employeeTree.replaceEmployees(changed);
        } catch (IOException error) {
            throw new UncheckedIOException("Could not persist employee records to employee-tree.json.", error);
        }
        employeeList = nextList;
        employeeSearchBst.rebuild(changed);
        verifyEmployeeStructures(employeeList.traverse(), employeeTree.inorder());
        App.synchronizeWorkloadsIfInitialized();
        return result;
    }

    /** Inserts through the persisted Binary Tree, then rebuilds its in-memory list view. */
    public synchronized Employee insertEmployeeInTree(Employee employee) throws IOException {
        List<Employee> next = new ArrayList<>(employees());
        next.add(employee);
        validateEmployees(next);
        Employee inserted = employeeTree.insert(employee);
        rebuildEmployeeListFromTree();
        App.synchronizeWorkloadsIfInitialized();
        return inserted;
    }

    /** Updates the tree node in place and refreshes the linked-list records after persistence succeeds. */
    public synchronized Employee updateEmployeeInTree(String id, Employee updated) throws IOException {
        List<Employee> next = new ArrayList<>(employees());
        boolean found = false;
        for (int index = 0; index < next.size(); index++) {
            if (next.get(index).id().equalsIgnoreCase(id == null ? "" : id.strip())) {
                next.set(index, updated);
                found = true;
                break;
            }
        }
        if (!found) throw new IllegalArgumentException("Employee ID was not found in this tree.");
        validateEmployees(next);
        Employee result = employeeTree.update(id, updated);
        rebuildEmployeeListFromTree();
        App.synchronizeWorkloadsIfInitialized();
        return result;
    }

    /** Deletes using the Binary Tree's deepest-rightmost replacement and refreshes the list. */
    public synchronized Employee deleteEmployeeFromTree(String id) throws IOException {
        Employee removed = employeeTree.delete(id);
        rebuildEmployeeListFromTree();
        App.synchronizeWorkloadsIfInitialized();
        return removed;
    }

    private void rebuildEmployeeListFromTree() {
        List<Employee> records = employeeTree.inorder();
        SinglyLinkedList<Employee> nextList = linkedList(records);
        verifyEmployeeStructures(nextList.traverse(), records);
        employeeList = nextList;
        employeeSearchBst.rebuild(records);
    }

    private static SinglyLinkedList<Employee> linkedList(List<Employee> records) {
        validateEmployees(records);
        SinglyLinkedList<Employee> list = new SinglyLinkedList<>();
        records.forEach(list::insert);
        return list;
    }

    private static void verifyEmployeeStructures(List<Employee> linkedRecords, List<Employee> treeRecords) {
        if (linkedRecords.size() != treeRecords.size())
            throw new IllegalStateException("Employee Linked List and Binary Tree are out of sync.");
        Map<String, Employee> employeesById = new HashMap<>();
        for (Employee employee : linkedRecords) {
            if (employeesById.putIfAbsent(employee.id().toLowerCase(Locale.ROOT), employee) != null)
                throw new IllegalStateException("Employee storage contains duplicate IDs.");
        }
        for (Employee employee : treeRecords) {
            if (!employee.equals(employeesById.remove(employee.id().toLowerCase(Locale.ROOT))))
                throw new IllegalStateException("Employee Linked List and Binary Tree are out of sync.");
        }
        if (!employeesById.isEmpty())
            throw new IllegalStateException("Employee Linked List and Binary Tree are out of sync.");
    }

    private static void validateEmployees(List<Employee> list) {
            Set<String> ids = new HashSet<>();
            Set<String> emails = new HashSet<>();
            for (Employee employee : list) {
                if (employee == null) throw new IllegalArgumentException("Employee record is required");
                if (!ids.add(employee.id().toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Employee ID already exists");
                if (!emails.add(employee.email().toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Employee email address already exists");
            }
    }

    public synchronized List<User> users() { return List.copyOf(users); }
    public synchronized <R> R changeUsers(Function<List<User>, R> action) {
        return update(users, action, list -> {
            Set<String> names = new HashSet<>();
            Set<String> linkedEmployees = new HashSet<>();
            Set<String> employeeIds = new HashSet<>();
            employees().forEach(employee -> employeeIds.add(employee.id().toLowerCase(Locale.ROOT)));
            for (User user : list) {
                if (!names.add(user.username().toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Username already exists");
                if ("EMPLOYEE".equals(user.role())) {
                    if (!employeeIds.contains(user.employeeId().toLowerCase(Locale.ROOT)))
                        throw new IllegalArgumentException("Employee account references a missing record");
                    if (!linkedEmployees.add(user.employeeId().toLowerCase(Locale.ROOT)))
                        throw new IllegalArgumentException("Employee already has a linked account");
                }
            }
        });
    }

    public synchronized List<EmployeeRequest> requests() { return List.copyOf(requests); }
    public Path employeeRequestsFile() { return requestFile; }
    public synchronized <R> R changeRequests(Function<List<EmployeeRequest>, R> action) {
        List<EmployeeRequest> changed = new ArrayList<>(requests);
        R result = action.apply(changed);
        validateRequests(changed);
        try {
            saveRequests(changed);
        } catch (IOException error) {
            throw new UncheckedIOException("Could not save employee request history.", error);
        }
        requests.clear();
        requests.addAll(changed);
        rebuildPendingLeaveQueue();
        return result;
    }

    private void validateRequests(List<EmployeeRequest> list) {
            Set<String> ids = new HashSet<>();
            Set<String> employeeIds = new HashSet<>();
            employees().forEach(employee -> employeeIds.add(employee.id().toLowerCase(Locale.ROOT)));
            for (EmployeeRequest request : list) {
                if (request == null) throw new IllegalArgumentException("Employee request is required.");
                if (!ids.add(request.id())) throw new IllegalArgumentException("Duplicate request ID");
                if (!employeeIds.contains(request.employeeId().toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Request references a missing employee");
                if (!Set.of("PENDING", "APPROVED", "REJECTED").contains(request.status()))
                    throw new IllegalArgumentException("Invalid request status");
            }
    }

    public synchronized void submitRequest(EmployeeRequest request) {
        Objects.requireNonNull(request, "request");
        if (!"PENDING".equals(request.status())) throw new IllegalArgumentException("New requests must be pending.");
        if (employees().stream().noneMatch(employee -> employee.id().equalsIgnoreCase(request.employeeId())))
            throw new NoSuchElementException("Employee record not found");
        changeRequests(list -> { list.add(request); return null; });
    }

    /** Returns the oldest pending leave application, or null when the leave queue is empty. */
    public synchronized EmployeeRequest oldestPendingLeaveRequest() {
        return pendingLeaveQueue.isEmpty() ? null : pendingLeaveQueue.peek();
    }

    public synchronized int pendingLeaveCount() { return pendingLeaveQueue.size(); }

    /** Persists the decision before removing the front leave application from the queue. */
    public synchronized EmployeeRequest processLeaveRequest(String id, String decision,
                                                               String reviewer, String note) throws IOException {
        if (!Set.of("APPROVED", "REJECTED").contains(decision))
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED.");
        if (pendingLeaveQueue.isEmpty()) throw new NoSuchElementException("No pending leave applications are waiting.");
        EmployeeRequest front = pendingLeaveQueue.peek();
        if (!front.id().equals(id))
            throw new IllegalArgumentException("Process the oldest pending leave application first.");

        List<EmployeeRequest> changed = new ArrayList<>(requests);
        int requestIndex = -1;
        for (int index = 0; index < changed.size(); index++) {
            EmployeeRequest request = changed.get(index);
            if (request.id().equals(id)) {
                if (!isPendingLeave(request)) throw new IllegalArgumentException("Leave application was already processed.");
                requestIndex = index;
                changed.set(index, new EmployeeRequest(request.id(), request.employeeId(), request.type(), request.details(),
                        decision, request.createdBy(), request.createdAt(), reviewer, Instant.now(), note,
                        request.leaveFrom(), request.leaveTo()));
                break;
            }
        }
        if (requestIndex < 0) throw new NoSuchElementException("Leave application was not found.");
        validateRequests(changed);
        saveRequests(changed);
        requests.clear();
        requests.addAll(changed);
        EmployeeRequest removed = pendingLeaveQueue.dequeue();
        if (!removed.id().equals(id)) throw new IllegalStateException("Pending leave queue changed during processing.");
        return changed.get(requestIndex);
    }

    private List<EmployeeRequest> loadRequests() throws IOException {
        if (!Files.exists(requestFile)) return new ArrayList<>();
        try {
            List<EmployeeRequest> loaded = requestJson.readValue(requestFile.toFile(), new TypeReference<List<EmployeeRequest>>() {});
            if (loaded == null) throw new IOException("Employee request history must be a JSON array.");
            validateRequests(loaded);
            return new ArrayList<>(loaded);
        } catch (RuntimeException error) {
            throw new IOException("Could not load employee request history from " + requestFile + ".", error);
        }
    }

    private void saveRequests(List<EmployeeRequest> records) throws IOException {
        Path parent = requestFile.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = requestFile.resolveSibling(requestFile.getFileName() + ".tmp");
        try {
            requestJson.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), records);
            try {
                Files.move(temporary, requestFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, requestFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void rebuildPendingLeaveQueue() {
        pendingLeaveQueue.clear();
        for (EmployeeRequest request : requests) if (isPendingLeave(request)) pendingLeaveQueue.enqueue(request);
    }

    private static boolean isPendingLeave(EmployeeRequest request) {
        return request.type().equalsIgnoreCase("Leave") && request.status().equals("PENDING");
    }
    public synchronized boolean deleteEmployeeIfUnreferenced(String id) {
        if (users.stream().anyMatch(user -> id.equalsIgnoreCase(user.employeeId()))
                || requests.stream().anyMatch(request -> id.equalsIgnoreCase(request.employeeId())))
            throw new IllegalStateException("Employee has linked accounts or request history");
        return changeEmployees(list -> list.removeIf(employee -> employee.id().equalsIgnoreCase(id)));
    }

    public synchronized List<String> departments() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(departments);
        employees().forEach(employee -> names.add(employee.department()));
        return List.copyOf(names);
    }
    public synchronized <R> R changeDepartments(Function<List<String>, R> action) {
        return update(departments, action, names -> {
            Set<String> unique = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (String name : names) if (name == null || name.isBlank() || name.length() > 80 || !unique.add(name.strip()))
                throw new IllegalArgumentException("Department names must be unique and 1-80 characters.");
        });
    }

    public synchronized List<RegistrationRequest> registrationRequests() { return List.copyOf(registrationRequests); }
    public synchronized <R> R changeRegistrationRequests(Function<List<RegistrationRequest>, R> action) {
        return update(registrationRequests, action, list -> {
            Set<String> ids = new HashSet<>();
            Set<String> names = new HashSet<>();
            Set<String> employeeIds = new HashSet<>();
            employees().forEach(employee -> employeeIds.add(employee.id().toLowerCase(Locale.ROOT)));
            for (RegistrationRequest request : list) {
                if (!ids.add(request.id())) throw new IllegalArgumentException("Duplicate registration request ID");
                if (!names.add(request.username().toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("A registration request already uses that username.");
                if (!employeeIds.contains(request.employeeId().toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Registration request references a missing employee");
            }
        });
    }

    /** Activities are kept newest-first in the custom linked-node stack. */
    public synchronized List<Activity> activity() { return activityStack.display(); }
    public synchronized void recordActivity(Activity entry) {
        activityStack.push(Objects.requireNonNull(entry));
        trimActivityStack();
    }
    public synchronized boolean activityIsEmpty() { return activityStack.isEmpty(); }
    public synchronized int activityCount() { return activityStack.size(); }

    /** Compatibility for existing callers; the supplied list is ordered top-to-bottom. */
    public synchronized <R> R changeActivity(Function<List<Activity>, R> action) {
        List<Activity> changed = new ArrayList<>(activityStack.display());
        R result = action.apply(changed);
        if (changed.size() > 2000) changed.subList(2000, changed.size()).clear();
        replaceActivityStack(changed);
        return result;
    }

    private void trimActivityStack() {
        if (activityStack.size() <= 2000) return;
        replaceActivityStack(activityStack.display().subList(0, 2000));
    }

    private void replaceActivityStack(List<Activity> topToBottom) {
        activityStack.clear();
        for (int i = topToBottom.size() - 1; i >= 0; i--) activityStack.push(topToBottom.get(i));
    }

    private static <T, R> R update(List<T> current, Function<List<T>, R> action,
                                   java.util.function.Consumer<List<T>> validate) {
        List<T> changed = new ArrayList<>(current);
        R result = action.apply(changed);
        validate.accept(changed);
        current.clear();
        current.addAll(changed);
        return result;
    }
}
