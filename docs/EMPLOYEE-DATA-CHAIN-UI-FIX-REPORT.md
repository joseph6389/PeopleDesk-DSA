# Employee Data Chain UI Fix Report

**Project:** PeopleDesk — Employee Record Management System  
**Date:** 31 October 2026  
**Scope:** Replace only the Singly Linked List overview card content and diagnose the Employee Data Chain 404.

## Root cause of “Not found”

The current source and freshly compiled classes register both `GET` and `POST /api/dsa/employee-chain`. `LocalServer.handle` dispatches authenticated `/api/*` requests to `ApiController`; `ApiController.get` and `post` strip the `/api` prefix and route `/dsa/employee-chain` to `EmployeeChainController`. The controller permits Admin and HR, and its GET response contains the `employees` and `initialIds` fields that `openEmployeeChain()` reads. The frontend calls `api('/dsa/employee-chain')`; the shared `api()` helper prefixes that with `api`, producing `/api/dsa/employee-chain`. Route, method, response shape and authorization are aligned in source.

The 404 occurs because the PeopleDesk Java server already running on port 8080 predates the route addition. A running JVM keeps its loaded `ApiController` class even after the project files are rebuilt, while its asset directory can serve the newer JavaScript. That allows the new view to open while the old Java handler returns its default “Not found” response. The fix is present in source and compiled output; the running process must be restarted to load it. I did not stop the existing server because doing so would discard its in-memory requests and activity.

## Singly Linked List card

The Phase 1 card now contains one button labelled **Employee Data Chain**. Its old heading, description, “Open Employee Data Chain” label, and employee ID preview were removed. The button still invokes the existing `openEmployeeChain()` view. No other DSA overview section or operation was edited.

## Files changed

- `src/main/webapp/assets/app.js` — replaced only the linked-list card content and removed the obsolete preview rendering.
- `src/main/webapp/assets/app.css` — added sizing/alignment for the Employee Data Chain button in that card.
- `docs/EMPLOYEE-DATA-CHAIN-UI-FIX-REPORT.md` — this report.
- `build/classes/**` — refreshed generated output from the successful test build.

The Employee Data Chain Java classes and routes (`SinglyLinkedList`, `EmployeeChainDemo`, `EmployeeChainController`, and the `ApiController` route dispatch) were inspected and left unchanged in this task. They continue to use a temporary `SinglyLinkedList<Employee>` constructed from existing PeopleDesk employee records; none of its actions writes to `AppData.employees`.

## Backup

Created before edits: `backup-before-chain-ui-fix-20261031.zip` in the project root. The archive was reopened and verified to contain 228 entries, including the pre-edit source, frontend and documentation.

## Tests and verification

| Check | Result | Detail |
|---|---|---|
| Java 17 build | **PASS** | `test.ps1` invoked `build.ps1`; build succeeded with Java 17. |
| Automated tests | **PASS** | 33 tests passed; 0 failed. This includes employee-chain operations and isolation tests, existing DSA tests, and existing authentication/HTTP tests. |
| JavaScript syntax | **PASS** | `node --check src/main/webapp/assets/app.js`. |
| Route/path/method/response source review | **PASS** | Frontend GET/POST calls match the registered API path; GET supplies `employees` and `initialIds`. Admin/HR authorization is consistent with the DSA page. |
| Insert, Search, Update, Delete, Traverse, Clear | **PASS — automated service tests** | Tested on the temporary list, including duplicate/unknown IDs, head/tail removal, empty-list behavior and clearing. |
| Employee record isolation | **PASS — automated service tests** | `AppData.employees()` remained equal to its original snapshot after demo operations. |
| Employee Data Chain browser/API integration | **NOT VERIFIED** | The existing process on port 8080 was not restarted; port 8081 was occupied, and a separate local server could not establish loopback access in this environment. |
| Back navigation and visible DSA overview | **NOT VERIFIED in browser** | Source check confirms Back toggles the view and overview containers without rebuilding the DSA page. |
| Visual page-level overflow | **NOT VERIFIED in browser** | The chain uses responsive styles; no browser layout pass was possible. |

## Other DSA sections

Stack, Queue, Expression Processing, Binary Tree traversals, BST and deletion, Graph and BFS, Searching and Sorting, and Greedy Algorithm markup and handlers were not changed. Their existing automated regression tests passed. I normalized the pre-edit `app.js` from the backup by applying only the linked-list card replacement and removing only its preview-rendering statement; the normalized file exactly matches the current `app.js`. All four phase headings remain present.

## Remaining action

Restart PeopleDesk using the project’s normal run workflow so the active JVM loads the current `ApiController`. Then verify the Employee Data Chain view and operations in Chrome. No other source defect was found in the current route, controller response, or frontend API handling.
