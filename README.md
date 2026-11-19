# PeopleDesk — Employee Record Management System

PeopleDesk is a local employee and staff-request application built with Java 17's built-in `HttpServer`, HTML, CSS, and vanilla JavaScript. Application data lives only in Java collections in the running process. There is no database or application data file storage. HTTP requests and responses still use JSON as their message format.

## Start the application

Requirements: Windows and JDK 17. From PowerShell in this folder, run:

```powershell
.\run.ps1
```

Each start initializes three fixed local demo accounts: Admin (`admin`), HR (`hr-admin`), and Employee (`employee`). Their passwords are represented in the application only by BCrypt hashes. The Employee demo account is linked to seeded employee record `EMP001`; it can sign in without registration or approval. These demo accounts are for the local college demonstration only.

The server binds to `http://127.0.0.1:8080/`; set `$env:EMPLOYEE_PORT` to use another port. Stop with Ctrl+C. Build separately with `.\build.ps1` (without the leading space) and run the test suite with `.\test.ps1`.

**Runtime changes reset on restart.** Every start recreates the 50 predefined employees, six departments, and three demo accounts. Runtime employee/account changes, departments, registration approvals, requests, and activity history last only until the server stops. The Employee Binary Tree persists employee nodes and left/right structure in `data/employee-tree.json`.

## Features

- Separate Admin, HR, and Employee dashboards with server-side authorization.
- Admin and HR login, linked Employee login, employee registration, and registration approval.
- BCrypt password hashing, in-memory sessions, role checks, employee ownership checks, and CSRF tokens.
- Employee create, edit, delete, search, status filtering, sorting, and 10-row pagination.
- Department management, employee requests and approvals, activity history, and employee profile updates.
- Employee and department statistics derived from current in-memory records.
- Annual salary in each employee record and a separate salary estimate calculator.
- DSA Laboratory with linked list, stack/queue demonstrations, expression conversion, a persistent Employee Binary Tree, search/sort, graph traversal, and greedy training allocation.

## Project layout

- `src/main/java/` — Java models, in-memory data, HTTP server, authentication, and algorithms.
- `src/main/webapp/` — HTML, CSS, and JavaScript frontend.
- `src/test/java/` — JUnit tests.
- `scripts/TestRunner.java` — direct JUnit runner; Maven and Gradle are not used.
- `lib/` — bundled Jackson for HTTP JSON messages and jBCrypt for password hashing. `lib/test/` contains JUnit.
- `docs/` — implementation, security, and project notes.

## Security and limits

PeopleDesk is for local college demonstration. It binds only to loopback and has no HTTPS or MFA. Login attempts are throttled by username after five failed attempts in one minute; the temporary block lasts one minute. The three startup demo accounts use BCrypt password hashes and never require registration. Other employee registrations still link to an existing active employee record and require HR/Admin approval before login. All state is temporary and is discarded when the server process stops.

See [security notes](docs/SECURITY.md), [DSA implementation](docs/DSA-IMPLEMENTATION.md), and [project map](PROJECT_MAP.md).
