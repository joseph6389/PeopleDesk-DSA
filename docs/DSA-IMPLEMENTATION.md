# DSA implementation map

Java executes DSA operations and returns their results to the browser. Employee records are persisted as an ordinary Binary Tree in `data/employee-tree.json`. At startup, `AppData` loads that tree and builds a synchronized in-memory `SinglyLinkedList<Employee>` in the tree's inorder traversal. Employee reads traverse the linked list; successful employee CRUD persists the tree first and then refreshes the linked list. The Binary Tree module searches and mutates the same tree. The list is not separately persisted, and there is no standalone Employee Data Chain page or API.

| Phase/use case | Java implementation | Complexity and notes |
|---|---|---|
| Employee storage representation | `dsa.SinglyLinkedList<Employee>` managed by `service.AppData` with `dsa.EmployeeBinaryTree` | Sequential linked-list reads and ID search; list is initialized from the tree's inorder traversal. Tree JSON remains canonical storage. Inserts use the first vacant child position, checking left before right; deletion uses deepest-rightmost replacement. CRUD changes are persisted before the list is rebuilt. |
| 2: Employee Organizational Tree | `dsa.EmployeeRecordBinaryTree`; GET `/api/dsa/tree?department=...&order=...` | Filters current employees by department, sorts IDs ascending, then inserts records into custom nodes by filling vacant child positions from left to right. Not a BST. |
| 2: Tree Traversal | `dsa.EmployeeRecordBinaryTree`; GET `/api/dsa/tree?department=...&order=...` | Uses the selected department and returns the selected Java traversal. Traversal O(n), recursion O(h). |
| 2: Employee Connection Graph and BFS | `dsa.EmployeeGraph`; GET `/api/dsa/graph` | Java adjacency list and BFS over configured employee vertices; BFS O(V+E). |
| 3: employee search and sorting | `dsa.EmployeeAlgorithms`; GET `/api/employees?search=…&status=…&sortBy=...&direction=...` | Linear search/filtering followed by stable Java Merge Sort; sorting works on a copy. |
| 4: employee workload distribution | `dsa.GreedyWorkloadAllocator`, `service.EmployeeWorkloadService`; `/api/dsa/workload-distribution` | Selects ACTIVE employees with lowest workload; tie-breaks by Employee ID. Workload and allocation history remain in their own JSON files. |

## Employee linked-list and tree synchronization

`AppData` owns the live `SinglyLinkedList<Employee>` and the canonical `EmployeeBinaryTree`. Each linked-list node holds an employee reference and a next link. Startup loads `data/employee-tree.json` into the Binary Tree, then constructs the linked list from its inorder traversal. Employee list and single-record reads, including linear search and employee ID lookup, traverse this list.

Employee create, update, and delete operations validate the requested records and persist changes through the Binary Tree. Once persistence succeeds, `AppData` rebuilds the linked list from the tree and checks that the employee sequences and unique IDs agree. If saving fails, the current list remains intact. The list is an in-memory representation only; it is never a second storage source. Employee Binary Tree API operations use Java tree methods and refresh the same linked list after successful mutations.

## UI behavior

The DSA Laboratory contains its existing Phase 2–4 modules. It has no standalone Employee Data Chain card, visualization, or demo endpoints. Other module layouts and the Employees page remain separate. Employee search and sorting call Java; the browser displays returned results.

## Automated coverage

The JUnit suite covers linked data structures, employee tree/list synchronization and persistence, employee search/sort, workload allocation, tree/graph behavior, authentication and HTTP request handling. Run it with `./test.ps1`.
