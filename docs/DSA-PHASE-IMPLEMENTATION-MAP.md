# PeopleDesk DSA Phase Implementation Map

This map follows the current Java and browser source. Java provides the data structures and algorithm results; the vanilla JavaScript page gathers demonstration input and renders those results. Employee records persist in `data/employee-tree.json`; `AppData` synchronizes its in-memory linked-list view with that Binary Tree. Other application state remains in Java in-memory collections.

## Employee Organizational Tree and Tree Traversal

Employee records are stored in `data/employee-tree.json`. `AppData` keeps one in-memory `SinglyLinkedList<Employee>` synchronized from the canonical employee Binary Tree for application reads and CRUD operations. The former standalone Employee Data Chain demonstration and API have been removed.

## Phase 2 — Hierarchical and Graph Structures

The DSA Laboratory overview retains the existing **Employee Organizational Tree** and **Tree Traversal** modules. Employee Organizational Tree opens a dedicated structure view; Tree Traversal opens a separate dedicated traversal view. Both use `EmployeeRecordBinaryTree`, do not alter employee storage, and do not imply manager-reporting relationships.

| Concept | PeopleDesk page/module | Java class and method | What it does | Judge demonstration | Complexity | Status |
|---|---|---|---|---|---|---|
| Employee Organizational Tree | DSA Laboratory → Employee Organizational Tree dedicated view | `EmployeeRecordBinaryTree.selectDepartment`, constructor, child insertion, `rootView` | Selects actual current employee records for a department, sorts by Employee ID, then builds an ordinary linked-node Binary Tree by filling available left child positions before right child positions. | Choose All Departments or an actual department, inspect employee nodes and child links, then select a traversal. Rebuilding reads current records and does not modify them. | Department selection/sort O(n log n); tree construction O(n); each traversal O(n); tree space O(n). | **Implemented — direct DSA** |
| Tree Traversal | DSA Laboratory → Tree Traversal dedicated view | `EmployeeRecordBinaryTree.inorder`, `preorder`, `postorder` through `/dsa/tree` | Selects a department and traversal; Java sorts records, builds the same ordinary Binary Tree shape, executes the selected traversal, and returns ID/name/department. | Change department or traversal; result order is produced by the Java tree links. | Build O(n log n) including sorting; traversal O(n); recursion O(h). | **Implemented — direct DSA** |
| Inorder | DSA Laboratory → Tree Traversal dedicated view | `EmployeeRecordBinaryTree.inorder` | Traverses the selected employee tree Left → Root → Right and returns each employee ID, name and department from Java. | Select Inorder and follow the returned employee sequence. | O(n) time, O(h) recursion space. | **Implemented — direct DSA** |
| Preorder | DSA Laboratory → Tree Traversal dedicated view | `EmployeeRecordBinaryTree.preorder` | Traverses the selected employee tree Root → Left → Right and returns each employee ID, name and department from Java. | Select Preorder and follow the returned employee sequence. | O(n) time, O(h) recursion space. | **Implemented — direct DSA** |
| Postorder | DSA Laboratory → Tree Traversal dedicated view | `EmployeeRecordBinaryTree.postorder` | Traverses the selected employee tree Left → Right → Root and returns each employee ID, name and department from Java. | Select Postorder and follow the returned employee sequence. | O(n) time, O(h) recursion space. | **Implemented — direct DSA** |

| Employee Connection Graph — Adjacency List | DSA Laboratory → Employee Connection Graph dedicated view | `EmployeeGraph.sampleEmployeeConnections`, `inducedSubgraph`, `addVertex`, `addEdge`, `adjacency` | Builds an undirected adjacency list over up to five actual employee IDs with isolated sample edges. All Employees is the default; selected IDs produce a validated induced subgraph. The SVG and optional adjacency table render the same Java graph. | Select employees and compare the diagram with the optional adjacency table. | Storage O(V+E); duplicate-edge check scans the relevant adjacency list. | **Implemented — direct DSA** |
| Breadth-First Search (BFS) | DSA Laboratory → Employee Connection Graph → Graph BFS | `EmployeeGraph.bfs` | Uses a `HashSet` of visited employee IDs and `LinkedQueue` frontier to visit reachable vertices level by level from a validated starting employee. Uses the same sample graph as the graph page. | Enter a starting employee ID and run BFS. | O(V+E) time, O(V) auxiliary space. | **Implemented — direct DSA** |

## Phase 3 — Searching and Sorting

| Concept | PeopleDesk page/module | Java class and method | What it does | Judge demonstration | Complexity | Status |
|---|---|---|---|---|---|---|
| Linear Search | Employees page search box | `EmployeeAlgorithms.linearSearch`; GET `/employees?search=…&status=…` | Scans each employee once and supports case-insensitive partial matches on employee ID, name, or department, combined with status filtering. | Search by full or partial ID/name/department, with optional status and sort controls. | O(n) scan, followed by the requested result sort. | **Implemented — direct DSA** |
| Merge Sort | DSA Laboratory → Phase 3; Employees table sorting | `EmployeeAlgorithms.mergeSort(List<Employee>)`, `mergeSort(List<Employee>, String, boolean)`, private `sort` | Stable recursive merge sort. The overload supports ID, name, department, salary and joining date, ascending or descending. It copies the input list and does not mutate employee records. | Sort each Employees field in both directions; use DSA Laboratory to show employee IDs ascending. | O(n log n) time and O(n) auxiliary space. | **Implemented — primary sorting algorithm** |

## Phase 4 — Greedy Algorithm

| Concept | PeopleDesk page/module | Java class and method | What it does | Judge demonstration | Complexity | Status |
|---|---|---|---|---|---|---|
| Greedy employee workload distribution | DSA Laboratory → Phase 4 | `GreedyWorkloadAllocator`; `EmployeeWorkloadService`; `/api/dsa/workload-distribution` | Assigns weighted work units to ACTIVE employees by repeatedly choosing the lowest current workload; ties use ascending Employee ID. Easy = 1, Medium = 2, Difficult = 3. Includes pending work units, workload comparison, allocation history and individual reset. | Add pending work units and run automatic allocation; review employee workloads and allocation history. | O(T × E) allocation; O(E + T) working space. | **Implemented** |

## Supporting application modules

| Module | Page / Java source | Actual data structures and behavior | DSA relationship and status |
|---|---|---|---|
| Employee Management | Employees page; `ApiController`; `AppData`; `EmployeeAlgorithms` | Employee reads traverse the synchronized `SinglyLinkedList<Employee>` backed by the persistent employee Binary Tree. Java handles search/filter/sorting before the browser renders returned rows and pagination. | Supporting application functionality. Search is not binary search on this page; Merge Sort directly supplies its order. |
| Account Access | Account Access page; `AccountController.handle`; `AppData.changeUsers` | Accounts are held in an `ArrayList`. Username/account lookup, role filtering and activation use stream scans or loops (linear in account count). Duplicate usernames are checked by scan and again by `HashSet` validation. HR sees Employee account entries only; Admin provisions HR accounts. There is no account `HashMap` index. | Supporting application functionality; no extra DSA algorithm is claimed. |
| Requests | My Requests / Requests and registrations; `EmployeeRequest`; `AppData.submitRequest`; `ApiController` | Request history is stored in an `ArrayList` appended in submission order. Pending decisions can only target the first pending entry. Leave requests carry validated ISO `leaveFrom` and `leaveTo` values; other request types remain date-free. | Supporting workflow uses list-backed FIFO ordering and server-side enforcement that only the oldest pending request can be decided. The DSA Queue demonstration is removed. |
| Activity Stack | Activity page; `App.log`; `AppData.recordActivity`, `activity`; `LinkedStack<Activity>` | Each new real application activity is pushed onto the custom linked-node stack. Reads return TOP-to-BOTTOM order; the visual page labels TOP and BOTTOM and connects activity nodes. | Supporting application feature shared by Admin and HR; read-only visualization. |
| Salary Calculator | Salary Calculator page; `salaryPage` in `assets/app.js` | Client-side arithmetic stores input as integer cents using `BigInt`; gross/net and annual values are derived directly. Inputs are validated; results are formatted in rupees. It does not save employee salary records and adds no tax/statutory rules. | Supporting calculator; it does not use Stack. The former stack-based expression demonstration has been removed from DSA Laboratory. |
| Authentication and role-based access | Login/dashboard; `AuthController.post`, `LocalServer.handle`, `Web.user`, `LocalSession` | Demo accounts and users are in in-memory `ArrayList` state. BCrypt verifies passwords; server-side sessions, CSRF checks, role checks and employee ownership checks protect API routes. | Supporting application security; no algorithmic DSA claim. |

## Not implemented or not claimed

- Depth-First Search (DFS), dynamic programming and backtracking are not implemented.
- Quick Sort and Insertion Sort are not included; Merge Sort remains the sorting algorithm.
- No account `HashMap` index is used.
- The Salary Calculator does not use a stack.
- The DSA Laboratory no longer includes its former Stack, Queue, or stack-based expression demonstrations. The application Activity Stack remains implemented with `LinkedStack<Activity>`.
- The traversal playback is UI state over the Java-generated sequence; it is not a separate traversal algorithm in JavaScript.

## Frontend locations

All DSA demonstrations are rendered from `src/main/webapp/assets/app.js` in the `page()` function's `current === 'dsa'` branch. That branch contains the existing Employee Organizational Tree and Tree Traversal entries and the Phase 2–4 headings and controls. The Employee Organizational Tree view requests `/dsa/tree` for a department and renders the returned linked-node shape. The separate Tree Traversal view requests `/dsa/tree` for a department and selected traversal and renders Java’s employee sequence. Both use the same Java tree implementation. The existing BST and graph remain separate demonstrations. Employee-table filtering, pagination and sort controls are in `drawEmployees()` and `reloadEmployees()`; the latter fetches the sorted Java result, while `drawEmployees()` applies client-side filtering and pagination. Salary arithmetic is in `salaryPage()`.

## Phase summary

| Phase | Algorithm/data structure | PeopleDesk location | Java implementation | Status |
|---|---|---|---|---|
| 2 | Employee Organizational Tree | DSA Laboratory → Employee Organizational Tree dedicated view | `EmployeeRecordBinaryTree` child insertion; department-filtered `/dsa/tree` route in `ApiController.get` | Implemented; reads current employee records without mutating storage |
| 2 | Tree Traversal | DSA Laboratory → Tree Traversal dedicated view | `EmployeeRecordBinaryTree.inorder`, `preorder`, `postorder`; `/dsa/tree` route in `ApiController.get` | Implemented; uses the same tree construction as the visual page |
| 2 | Employee Connection Graph | DSA Laboratory → Employee Connection Graph dedicated view | `EmployeeGraph.sampleEmployeeConnections`, `inducedSubgraph`, `adjacency`; `/dsa/graph` and `/dsa/graph?ids=...` | Implemented; sample edges are isolated from actual employee relationships |
| 2 | Breadth-First Search | DSA Laboratory → Employee Connection Graph → Graph BFS | `EmployeeGraph.bfs`; `/dsa/graph?start=...` | Implemented; runs on the shared Java adjacency list using `LinkedQueue` |
| 3 | Employee list linear search | Employees page search box | `EmployeeAlgorithms.linearSearch`; `/employees` in `ApiController.get` | Implemented |
| 3 | Stable Merge Sort | Employees page and DSA Laboratory → Phase 3 | `EmployeeAlgorithms.mergeSort` overloads; `/employees` and `/dsa/sort` in `ApiController.get` | Implemented |
| 4 | Greedy employee workload distribution | DSA Laboratory → Phase 4 | `GreedyWorkloadAllocator`; `EmployeeWorkloadService`; `/api/dsa/workload-distribution` | Implemented; persists workload and allocation history |

The API route details above refer to the matching `get` or `post` method in `src/main/java/edu/college/employee/web/ApiController.java`. The employee tree is a normal Binary Tree that fills vacant child positions from left to right. Stack and Queue cards and stack-based expression processing are no longer part of the DSA Laboratory. `LinkedStack` remains shared by Activity Stack; `LinkedQueue` remains shared by graph BFS. Requests retain their separate list-backed FIFO behavior and first-pending decision guard. Supporting application pages such as Account Access and Salary Calculator are intentionally not counted as DSA demonstrations.



