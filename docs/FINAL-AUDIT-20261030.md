# PeopleDesk End-to-End Audit

**Audit completed:** 31 October 2026  
**Scope:** Existing application only; no feature or architecture rebuild.

## 1. Overall status

**Recommendation: suitable for a college demonstration, with limits.** The Java 17 build, all 26 automated tests, JavaScript syntax check, scripted localhost API checks, desktop browser checks, and restart/reset check passed where run. The application preserved its existing Java HttpServer, vanilla HTML/CSS/JavaScript, in-memory collections, 50 fictional employees, six departments, and green PeopleDesk theme.

No application-code defect was confirmed during this audit, so no Java, HTML, CSS, or JavaScript behavior was changed. Two documentation inconsistencies were corrected. Mobile/tablet browser layouts, browser console logs, and several role-specific screens were not exhaustively rechecked; see the limitations below.

## 2. Implemented features reviewed

- Admin, HR, and Employee roles with server-side authorization, employee ownership checks, BCrypt password hashes, server-side sessions, CSRF checks, and account deactivation.
- Admin/HR sign-in, Admin HR-account provisioning, employee login provisioning, employee self-registration, and HR/Admin registration review.
- Employee records, search/filter/sort/pagination UI, department management, account access, employee requests, activity history, salary estimation, and the DSA Laboratory.
- Java implementations for linked list, linked stack, linked queue, expression conversion, binary tree traversals, employee BST, department-membership graph/BFS, linear and binary search, merge sort, and greedy training allocation.
- Runtime data is held in Java collections. No application JSON file storage or database was found in the reviewed architecture.

## 3. Bugs and corrections

### Found

1. README and security notes said login throttling was absent even though `AuthController` implements it and `LoginThrottleTest` covers it.
2. DSA implementation notes linked verification results to a missing `PROGRESS.md`.

### Fixed

- Updated the security documentation to describe the five-failure, one-minute-window throttle and its one-minute block.
- Updated the DSA notes to link to this audit report.
- No application-code bug was confirmed; no application code was modified.

## 4. Files modified and backup

Modified:

- `README.md` — corrected the security-limit description.
- `docs/SECURITY.md` — documented the implemented login throttle.
- `docs/DSA-IMPLEMENTATION.md` — replaced the broken `PROGRESS.md` reference.
- `docs/FINAL-AUDIT-20261030.md` — this report.

Before editing, the three pre-existing documents were copied to `audit-backup-20261030/` in the project root. No Git metadata was present in the project directory.

## 5. Authentication and registration

| Check | Result |
|---|---|
| Admin login and dashboard | **PASS** — successful browser sign-in; dashboard showed 50 employees, six departments, and the configured HR account. |
| HR provisioning by Admin | **PASS** — temporary HR account provisioned and used to sign in through the API. |
| Employee registration | **PASS** — registration for a matching active fictional record entered the pending workflow. |
| Pending approval and pre-approval sign-in | **PASS** — the pending employee could not sign in before review. |
| HR approval and employee login | **PASS** — HR approved; Employee login and own dashboard succeeded. |
| Employee ownership and role escalation | **PASS** — Employee could read their own dashboard; staff listing and registration decisions returned 403. |
| Invalid credentials | **PASS** — returned 401. |
| Weak password | **PASS** — registration returned 400. |
| Duplicate pending registration | **PASS** — second request for the same employee returned 400. |
| Login throttling | **PASS** — sixth rapid failed attempt returned 429; the existing automated throttle tests also passed. |
| Account deactivation/session invalidation | **PASS** — Admin deactivated the temporary HR account; its existing session then received 401. |
| Logout | **PASS** — browser Admin sign-out returned to the login page. |

HR review, employee account approval, and request decisions were API-tested. The full registration form was not completed through browser UI during this audit.

## 6. Employee management

- **PASS:** Startup and dashboard showed 50 predefined records; IDs and emails were unique.
- **PASS:** Scripted create returned 201 and count rose to 51; edit returned 200; delete returned 200 and restored the count to 50.
- **PASS:** Duplicate ID and invalid employee input returned 400.
- **PASS:** Department field, salary and joining date were represented in records; employment status validation uses the supported ACTIVE, INACTIVE, and ON_LEAVE values.
- **PASS:** Browser showed 10 rows per page and “Page 1 of 5”; Next displayed rows 11–20.
- **PASS:** Browser search with no match displayed a clear empty state and clear-filter action.
- **PASS:** Browser status filter displayed 42 active records; name sort worked in ascending and descending directions with a matching indicator.
- **NOT VERIFIED:** Every combination of search field, sort key, and status filter was not exhaustively exercised in the browser.

## 7. Department management

- **PASS:** Six initial departments displayed and API returned six names.
- **PASS:** Duplicate department creation was rejected (400); adding and renaming an unused department succeeded.
- **PASS:** Deleting Engineering while employees remained assigned was rejected (409).
- **PASS:** Renaming a department containing a test employee updated the employee’s department; the renamed department statistic showed one employee.
- **PASS:** Deleting an empty department succeeded; test employee removal restored the employee total and the original six department names.

## 8. Registration and employee-request workflows

- **PASS:** Pending employee registration was reviewed and approved by HR, then the employee signed in.
- **PASS:** Employee request submission succeeded; HR rejected the request; the employee saw status REJECTED.
- **PASS:** Unauthorized employee registration decisions returned 403; anonymous request decisions returned 401.
- **PASS:** Activity entries were visible for employee creation, department changes, and other operations during the process lifetime.
- **PASS:** Re-review of a decided registration is rejected as already reviewed.
- **PARTIAL:** Notification behavior was observed through UI messages and status updates, but not every notification path or cancellation option was exercised. No cancellation workflow was found in the reviewed request API.

## 9. Salary Calculator

- **PASS:** Browser calculation with basic ₹1,000.25, allowances ₹100.50, other earnings ₹20, bonus ₹10, and deductions ₹30.75 produced gross monthly ₹1,130.75, net monthly ₹1,100.00, annual gross ₹13,569.00, and annual net ₹13,200.00.
- **PASS:** Negative input displayed a clear validation message; Clear returned values and calculated totals to zero.
- **PASS:** The UI states that no tax or statutory deductions are calculated. The formulas match the implemented rules: gross is the sum of earnings; net is gross less deductions; annual estimates multiply monthly values by 12.
- **NOT VERIFIED:** Very large values, malformed decimal precision, and every individual empty/zero input combination were not exhaustively tested in a browser.

## 10. DSA Laboratory

- **PASS:** All DSA JUnit tests passed, covering list operations, stack LIFO/underflow, queue FIFO/underflow, expression processing, tree traversals, BST deletion shapes, graph/BFS, search/sort, and training allocation.
- **PASS:** Browser DSA demonstration rendered all 50 employee IDs in the linked list, employee BST in-order sequence, the general tree traversal, and the current department membership graph (56 vertices, 50 edges).
- **PASS:** API expression check `(12+3)*4` returned postfix `12 3 + 4 *`; BST deletion removed the selected ID from the demo tree; merge sort returned 50 employees.
- **PASS:** Training allocation selected a feasible item under the submitted budget. The implementation describes itself as a greedy heuristic, not a globally optimal solver.
- **PARTIAL:** Stack and queue mechanics were verified by automated tests; the UI demonstrates the list and pending-request FIFO queue rather than exposing separate stack controls.

## 11. UI and responsive behavior

- **PASS:** Real desktop browser checks covered Admin dashboard, employee table, pagination, empty search state, status filtering, ascending/descending sorting, Salary Calculator, DSA Laboratory, and logout.
- **PASS:** Existing keyboard labels, focus styles, modal dialog markup, accessible search/filter names, notifications, and confirmation-dialog code were reviewed in source.
- **PARTIAL:** HR and Employee dashboards, registration review, account management, request review, department page, activity page, and dialog keyboard flows were checked through API/source review but were not all manually navigated in the browser.
- **NOT VERIFIED:** Tablet and mobile viewport behavior, horizontal overflow at those sizes, exhaustive keyboard navigation, and browser console logs. The JavaScript syntax check passed, and no browser error was observed during the pages exercised, but a console-log capture was unavailable in this browser session.

## 12. In-memory storage and restart

- **PASS:** Startup printed `Loaded employees: 50`; API verified 50 unique employee IDs and 50 unique emails.
- **PASS:** Six seeded departments loaded.
- **PASS:** Demo Admin and HR accounts were initialized and could sign in.
- **PASS:** Runtime employee and department changes, requests, and activity worked while the server was running.
- **PASS:** After stopping and restarting, employee count returned to 50, departments returned to six, and temporary accounts/changes were gone; only configured `admin` and `hr-admin` accounts remained.
- The reset is expected for this project and is not treated as a defect.

## 13. Build and tests

- **Java 17 build:** **PASS** — run through `test.ps1` after documentation updates.
- **Automated tests:** **PASS** — 26 passed, 0 failed, 0 skipped.
- **JavaScript syntax:** **PASS** — `node --check src/main/webapp/assets/app.js`.
- **API integration:** **PASS** for the scripted cases listed above, against the running local Java server.
- **Browser:** **PASS** for the desktop pages/interactions listed above; see responsive and console limitations.
- **Restart:** **PASS** — seeded data and accounts reset as intended.
- **Terminal/server errors:** **PASS** for build/test output and observed server responses during the exercised flows. Console-log capture was not available; see UI limitations.

## 14. Remaining limits and risks

- The app is a localhost college demonstration, not a production HR system. It has no HTTPS, MFA, database, or persistence; all runtime state resets on restart.
- Full mobile/tablet visual QA and browser console capture remain unverified.
- Some role-specific pages and dialogs were not exhaustively exercised manually in the browser.
- Scripted manual API integration checks are not included as permanent automated integration tests.

**Final recommendation:** PeopleDesk is ready for a college demonstration using configured demo credentials. Do a quick presentation-device check at the intended screen size and keep runtime changes disposable, since restart resets them.
