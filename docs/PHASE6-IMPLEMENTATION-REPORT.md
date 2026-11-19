# Phase 6 Implementation Report

**Project:** PeopleDesk — Employee Record Management System  
**Implementation date:** 31 October 2026  
**Scope:** Focused improvements to leave requests, FIFO processing, employee sorting and the DSA Laboratory. Existing architecture and role controls were preserved.

## 1. Summary of changes

- Added validated `Leave From` and `Leave To` fields for Leave requests, including request-history rendering and preservation through decisions.
- Corrected request ordering to oldest-submitted first and enforced decisions against the first pending request.
- Kept Merge Sort as the project sorting algorithm and routed Employees-page field/direction sorting through the Java implementation.
- Reorganized the DSA Laboratory into the four requested phases and replaced the confusing employee-ID tree output with a stepwise A–F traversal demonstration.
- Verified the Salary Calculator source and browser behavior; it uses ordinary integer-cent arithmetic and does not use a stack or modify employee salary records.
- Added focused tests for leave-date validation, FIFO insertion order, and the supported Merge Sort fields and directions.

No database, persistence framework, JSON file storage, external algorithm library, new Java class or new JavaScript file was introduced. No existing project file was deleted.

## 2. Leave date-range implementation

`EmployeeRequest` now carries optional ISO `leaveFrom` and `leaveTo` strings. For Leave requests, the model and API require both dates, parse them as `LocalDate`, and reject malformed dates or an end date before the start date. Same-day leave is valid. The browser shows required date inputs for Leave, sets the end-date minimum from the start date, displays an inline validation message, and hides/unrequires the dates for other request types.

Submitted requests and HR/Admin review rows display the date range. Decision updates preserve the original range and approval/rejection behavior. Other request types continue to work without date fields. Employee ownership and server-side HR/Admin decision checks remain in place.

## 3. Employees page algorithm findings

- The text search is **not binary search**. `drawEmployees` scans employee records with JavaScript `filter` and `some`, matching ID, name, department, designation and email. With a fixed number of searchable fields, this is O(n).
- Status filtering also uses JavaScript `filter` and is O(n).
- Pagination uses `slice` after filtering and sorting; search, status and sort changes reset to page one. Page size remains 10.
- Before Phase 6, the table used JavaScript's native `.sort`, while the DSA Merge Sort sorted IDs only.
- The table now requests Java `EmployeeAlgorithms.mergeSort(input, field, ascending)` for ID, name, department, salary and joining date, ascending or descending. Filtering and pagination remain in the browser.
- Merge Sort is stable, runs in O(n log n) time and uses O(n) auxiliary space. It sorts a copy and leaves the in-memory employee records unchanged. It is suitable for this fixed, modest employee dataset and is already a required DSA demonstration. No Quick Sort or Insertion Sort was added.

## 4. Account Access findings

Users are stored in an `ArrayList` in `AppData`. Account/username lookup and role filtering use streams or loops, so lookup is linear in the number of accounts. Role is stored as a string on `User`; there is no account `HashMap` index. Duplicate usernames are checked in the provisioning flow and validated again using `HashSet` uniqueness checks in `AppData.changeUsers`. Activation/deactivation scans the user list and updates the matching record.

HR's account list exposes Employee accounts only. HR can manage permitted Employee access but cannot provision HR accounts or activate/deactivate Admin accounts. Admin retains HR-account provisioning. These server-side checks were preserved.

## 5. FIFO request implementation

Request history remains in one `ArrayList<EmployeeRequest>`. `AppData.submitRequest` now appends new requests, retaining submission order. The DSA structures endpoint enqueues pending IDs in that order into the existing `LinkedQueue` and dequeues them as its FIFO demonstration. The Requests page displays oldest submissions first. The server-side decision route accepts only the first pending entry, so a newer pending request cannot be approved/rejected ahead of an older pending request. Decided requests stay in history and keep their date range.

This uses the existing `LinkedQueue`; no duplicate queue class or extra persisted data structure was added.

## 6. Activity History findings

Activity entries are `Activity` records held in an `ArrayList` in `AppData`. `App.log` inserts each new entry at index zero. `AppData.activity()` returns a copy in that insertion order, and the UI displays the returned order, which is newest-first. There is no timestamp sort, search or filter operation. Activity history is not implemented as a Stack, Queue or LinkedList.

## 7. Sorting decision and justification

Merge Sort remains the sole explicit sorting algorithm. The employee table now uses the generic stable Java Merge Sort overload for each supported sort key and direction. The existing DSA `/api/dsa/sort` route still demonstrates ascending employee-ID Merge Sort. Search, filtering, pagination and employee records remain separate from the sort operation. No other sorting algorithm was removed because none was found as an explicit project implementation.

For n records, Merge Sort has O(n log n) time in all input orders, O(n) auxiliary storage, and stable ordering for equal keys. It is predictable and easy to demonstrate with the project's employee collection.

## 8. Tree traversal redesign

The DSA Laboratory now has separate Inorder, Preorder and Postorder selection, a tree diagram, Current/Visited/Unvisited colors, a step count, per-node explanation, visited-so-far sequence, final sequence, Reset and Next Step controls. The example tree is:

```text
        A
       / \
      B   C
     / \  /
    D  E F
```

The backend constructs this tree with the existing Java `BinaryTree` class and returns the selected algorithm's sequence. The browser steps over that returned sequence; it does not replace the Java traversal implementation. Verified expected sequences:

| Traversal | Rule | Result |
|---|---|---|
| Inorder | Left → Root → Right | D → B → E → A → F → C |
| Preorder | Root → Left → Right | A → B → D → E → C → F |
| Postorder | Left → Right → Root | D → E → B → F → C → A |

The BST class and its leaf, one-child and two-child deletion behavior were retained.

## 9. Salary Calculator findings

The Salary Calculator is `salaryPage` in `assets/app.js`. It validates non-negative values with at most two decimal places and uses `BigInt` integer cents for Basic monthly salary, Allowances, Overtime/other earnings, Bonus and Deductions. Gross is the sum of earnings; Net is Gross minus deductions; annual figures multiply monthly Gross/Net by 12. It does **not** use `LinkedStack` or `ExpressionProcessor`, does not save employee records and does not add tax/statutory deduction rules. Stack-based expression parsing exists only in the DSA Laboratory.

## 10. DSA Laboratory improvements

The existing concepts are now grouped under four visible phase headings:

1. **Linear Data Structures:** Singly Linked List, Stack, Queue and stack-based expression conversion.
2. **Non-Linear Data Structures:** Binary Tree traversals, adjacency-list department graph and BFS.
3. **Searching and Sorting:** Employee list Linear Search is available in the Employees page search box; Phase 3 demonstrates Merge Sort.
4. **Greedy Algorithm:** Employee workload distribution using weighted work units and current workload.

No DFS, dynamic programming, backtracking, Quick Sort, Insertion Sort or additional greedy algorithm was introduced. See [DSA-PHASE-IMPLEMENTATION-MAP.md](DSA-PHASE-IMPLEMENTATION-MAP.md) for classes, methods, complexity and judge demonstrations.

## 11. Four-phase implementation mapping

The complete implementation map is in [DSA-PHASE-IMPLEMENTATION-MAP.md](DSA-PHASE-IMPLEMENTATION-MAP.md). It separates direct DSA implementations from supporting Employee Management, Account Access, Requests, Activity History, Salary Calculator and Authentication functionality, and identifies the requested-but-unimplemented concepts.

## 12. Files changed

**Java source**

- `src/main/java/edu/college/employee/model/EmployeeRequest.java`
- `src/main/java/edu/college/employee/service/AppData.java`
- `src/main/java/edu/college/employee/web/ApiController.java`
- `src/main/java/edu/college/employee/dsa/EmployeeAlgorithms.java`

**Frontend**

- `src/main/webapp/assets/app.js`
- `src/main/webapp/assets/app.css`

**Tests**

- `src/test/java/edu/college/employee/service/AppDataTest.java`
- `src/test/java/edu/college/employee/dsa/EmployeeAlgorithmsTest.java`

**Documentation**

- `docs/DSA-IMPLEMENTATION.md` — corrected its tree and UI descriptions.
- `docs/DSA-PHASE-IMPLEMENTATION-MAP.md` — added the requested phase and supporting-module mapping.
- `docs/PHASE6-IMPLEMENTATION-REPORT.md` — this report.

No files were removed. The build refreshed generated content in `build/classes/**`.

## 13. Backup

The full pre-change project backup was verified before editing:

`C:\Users\josep\Downloads\COLLEGE PROJ\ds chat\backup-before-phase6-20261031.zip`

The archive contains 199 entries and is 57,312,848 bytes. It includes the pre-Phase-6 source and audit/documentation files.

## 14. Build, automated tests and manual testing

The source descriptions and file list above were checked against the current project during this documentation pass. The browser/manual results below were reported by the preceding Desktop Work session; they were not independently repeated here. This pass reran the local Java build, all automated tests, and the JavaScript syntax check.

| Check | Result | Detail |
|---|---|---|
| Java 17 build | **PASS** | `build.ps1` and the build invoked by `test.ps1` succeeded. |
| Automated suite | **PASS** | 29 tests passed, 0 failed. Existing tests were retained; focused leave/FIFO/Merge Sort tests were added. |
| JavaScript syntax | **PASS** | `node --check src/main/webapp/assets/app.js`. |
| Admin, HR and Employee browser login | **PASS** | All three roles reached their role-appropriate dashboards; Employee displayed `EMP001`'s seeded person. |
| Leave request date checks | **PASS** | Browser blocked missing/reversed ranges; valid range displayed to Employee and HR; HR approval retained it. A General request without dates also succeeded. API rejected missing/reversed dates with HTTP 400. |
| FIFO requests | **PASS** | History and queue matched submission order; deciding a newer pending request first returned 400; sequential oldest-first decisions succeeded. |
| Employee table | **PASS** | Search empty state, Active filter (42 records), sort direction/indicator and page 2 of 5 were exercised. API returned monotonic ascending/descending results for all five sort fields. |
| Accounts and roles | **PASS** | HR Account Access page omitted HR provisioning; HR API list contained Employee accounts only; HR provisioning/Admin deactivation and Employee-wide/private routes were denied. |
| Activity / departments | **PASS** | Both pages opened; six seeded departments displayed. Activity entries appeared newest-first. |
| Salary Calculator | **PASS** | Sample yielded gross ₹3,800, net ₹3,500, annual gross ₹45,600 and annual net ₹42,000. Negative input showed validation. EMP001 salary was unchanged before/after the calculation. |
| DSA demonstrations | **PASS** | Browser exercised all four phase headings; each traversal's complete sequence, current/visited/unvisited states, final disabled state and Reset; linear/binary search; Merge Sort; expression conversion; BST delete; BFS; greedy allocation. Existing JUnit tests cover BST deletion cases. |
| Logout/session invalidation | **PASS** | Admin, HR and Employee browser logout returned to login. API logout invalidated the session; `/api/me` then returned 401. |
| Restart/reset | **PASS** | After restart, all three logins worked; 50 employees, six departments and three demo accounts loaded; runtime requests/registrations reset; IDs/emails were unique. |
| Browser console | **NOT VERIFIED** | Browser developer console could not be inspected. Direct CSS/JS/favicon HTTP responses were not used as a substitute for console review. |
| Layout | **PASS / NOT VERIFIED** | Desktop-width (1280 CSS px) and narrow mobile-width (304 CSS px) browser layouts were inspected; tested screens had no page-level horizontal overflow. Tablet-width layout was **NOT VERIFIED**. |

**Port note:** `run.ps1` found port 8080 occupied by a pre-existing PeopleDesk process and refused to stop it. The updated source was verified at `http://127.0.0.1:8081/` using the supported `EMPLOYEE_PORT` setting. The 8080 process was left untouched. The Phase 6 test instance was stopped after checks.

## 15. Outstanding issues

- Browser console and unresolved resource messages remain **NOT VERIFIED** because DevTools console access was unavailable.
- Tablet-width layout remains **NOT VERIFIED**; desktop and narrow mobile widths were checked.
- Runtime requests, activities and other changes intentionally reset after the process stops because the application uses in-memory collections.

No unresolved application defect was reproduced in the implemented Phase 6 behaviors.
