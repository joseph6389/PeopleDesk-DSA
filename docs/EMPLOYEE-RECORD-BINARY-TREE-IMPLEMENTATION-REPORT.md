# Employee Record Binary Tree Implementation Report

## Malformed query parameter investigation (2026-11-02)

- **Root cause:** the tree page sends both `department` and `order` in one GET query. The older single-field `ApiController.query(raw, key, fallback)` parser accepts only one named parameter and throws `IllegalArgumentException("Malformed query parameters")` when it encounters a second field or a different field name. That is the source of the message; an empty `department` value by itself is valid. The `/dsa/tree` handler must use the multi-field parser.
- **Current source fix:** `app.js` calls `api(`/dsa/tree?department=${encodeURIComponent(department)}&order=${encodeURIComponent(order)}`)`. `ApiController` handles GET `/dsa/tree` through `queryParameters(q.getQuery(), Set.of("department", "order"))`, defaulting an absent department to the all-departments value `""` and traversal to `inorder`. `queryParameters` decodes UTF-8 values, accepts empty values, and rejects unknown or repeated keys. The route then filters current employees, builds the existing `EmployeeRecordBinaryTree`, and executes the selected Java traversal. No tree implementation or page design was replaced.
- **Runtime limitation:** the earlier implementation report records that the previously running JVM had older loaded classes and was intentionally not restarted to preserve in-memory data. In this verification session, no Java process or listener on port 8080 was observable from the available process checks, so a live before/after HTTP status and body could not be captured. This report does not claim a live API or authenticated browser pass. Once the application is launched from the current build, it will load the corrected multi-parameter handler.
- **Regression coverage:** `WebRequestParsingTest` covers encoded department names plus traversal, empty department plus traversal, and rejection of unknown, duplicate, and malformed values.

## What changed

The Phase 2 generic A–F example tree was replaced in the existing tree card with **Employee Record Binary Tree**. The DSA Laboratory heading now identifies Phase 2 as Hierarchical and Graph Structures. The feature provides an All Departments/department selector, an employee tree visualization, and an Inorder, Preorder, or Postorder selector. Tree nodes show actual Employee ID, name, and department; visible lines connect parent and child nodes. The viewport scrolls horizontally and vertically for larger trees.

## Implementation

- `EmployeeRecordBinaryTree` is a separate Java implementation with private linked `Node` objects. Each node stores an immutable PeopleDesk `Employee` record and has explicit `left` and `right` child references.
- `selectDepartment` filters current employee records (case-insensitively) and sorts by Employee ID. The constructor sorts its input by Employee ID as well, then inserts each record into the next vacant child position.
- Tree insertion keeps an auxiliary queue of parents with open positions. It fills a parent's left child first, then its right child. This is an ordinary Binary Tree, not a BST, and it does not guarantee perfect height balance for every count.
- `inorder`, `preorder`, and `postorder` walk the actual linked nodes in Java and return employee summary records. The `/dsa/tree` GET route returns the tree shape, selected traversal sequence, count, and the current department list.
- Every route request reads the current `AppData.employees()` snapshot, so the seeded 50 records and session-added records are eligible after a rebuild. The tree is temporary; it does not alter employee storage.
- The existing generic `BinaryTree<T>` class remains available for its generic data-structure test; it does not drive the persistent Employee Binary Tree.

## Files changed

- Added `src/main/java/edu/college/employee/dsa/EmployeeRecordBinaryTree.java`.
- Updated `src/main/java/edu/college/employee/web/ApiController.java` for department filtering, tree construction, traversal output, and the two-parameter query parser.
- Updated `src/main/webapp/assets/app.js` for the department selector, visual tree, scrolling viewport, and Java-returned traversal display.
- Updated `src/main/webapp/assets/app.css` for the PeopleDesk-green nodes, connectors, responsive controls, and internal scrolling.
- Added `src/test/java/edu/college/employee/dsa/EmployeeRecordBinaryTreeTest.java`.
- Updated `src/test/java/edu/college/employee/web/WebRequestParsingTest.java` for department plus traversal query parsing.
- Updated `docs/DSA-IMPLEMENTATION.md` and `docs/DSA-PHASE-IMPLEMENTATION-MAP.md`.
- Created this report. Build outputs were regenerated under `build/`.

## Backup

Pre-edit backup: `backup-before-employee-record-binary-tree-20261102.zip` (352,173,859 bytes). The archive contains 231 entries and was checked for the DSA renderer/styles, employee model, API controller, old BinaryTree/BST classes, and existing tree tests; all required entries were present.

## Verification results

- **PASS — Java 17 build:** `test.ps1` completed successfully.
- **PASS — Automated suite:** 40 tests passed, 0 failed (includes the empty All Departments query regression test).
- **PASS — JavaScript syntax:** `node --check src/main/webapp/assets/app.js` succeeded.
- **PASS — Tree logic:** tests verify Employee ID sorting without changing source order, linked left/right placement, all three traversal sequences, and inserting an additional node at the next open child position.
- **PASS — Data selection and isolation:** tests verify department filtering, All Departments selection, empty-tree behavior, all 50 seeded employee records, inclusion of a session-added 51st record after rebuild, and unchanged application storage.
- **PASS — Existing BST regression suite:** existing insertion, searching, deletion-shape and traversal checks remain and passed in the 39-test run.
- **PASS — Role guard source inspection:** `/dsa/tree` still calls the existing `hr(u)` guard, which permits Admin and HR; no role rules were edited.
- **PASS — Query parser source and unit coverage:** the route accepts `department` and `order` together; `""` means All Departments and the frontend encodes both values. The parser regression tests cover encoded spaces, the empty department value, duplicate keys, unknown keys, and invalid encoding.
- **PASS — Scope/source inspection:** Employee Data Chain, Activity Stack storage, Graph/BFS, Phase 3, Phase 4, and Requests FIFO code remain in source. The BST implementation was not edited.
- **NOT VERIFIED — Authenticated Admin/HR browser walkthrough, browser console, and visual scrolling/layout:** the available browser is at the sign-in page and the computer-use workflow does not permit automating sign-in. No visual browser result is claimed.
- **NOT VERIFIED — Live HTTP response from the running server:** current process/port checks did not expose a Java listener on port 8080. No server was stopped or restarted, and no live status/body is claimed.
- **NOT VERIFIED — Admin/HR authenticated browser request, Network panel, and console:** browser UI inspection showed Chrome open to an unrelated page; automated login is not performed.

## Remaining limitation

Launch PeopleDesk from the current build to load the multi-parameter tree handler, then verify the request in the browser as Admin and HR. That browser/network verification remains outstanding; the Java build, automated tests, and JavaScript syntax check are complete.
