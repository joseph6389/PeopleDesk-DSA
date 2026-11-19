# PeopleDesk project map

## Start here

- `service/LocalServer.java` starts Java 17's built-in loopback HTTP server.
- `service/App.java` creates one shared in-memory data collection for the server process.
- `service/AppData.java` owns employees, users, departments, registration requests, employee requests, and activity history in `ArrayList` collections. It initializes the three BCrypt-hashed demo accounts, including Employee linked to active `EMP001`. Mutations are validated before replacing list contents.
- `service/DemoSeed.java` defines the 50 fictional employees and creates them afresh whenever `AppData` is constructed.
- `web/AuthController.java`, `AccountController.java`, `RegistrationController.java`, and `ApiController.java` handle sign-in, account provisioning, employee registration, and application routes.
- `model/` contains the immutable employee, user, request, registration, and activity records.
- `dsa/` contains linked list, stack/queue, tree, graph, search/sort, expression, and allocation implementations.
- `web/Web.java` serializes HTTP JSON messages and checks active server-side sessions. JSON is only an API format; no application JSON files are read or written.

## Frontend and tests

- `src/main/webapp/` — HTML, CSS, and vanilla JavaScript.
- `src/test/java/` — JUnit tests, including the in-memory dataset and collection behavior.
- `scripts/TestRunner.java` — JUnit Platform launcher without Maven or Gradle.
- `lib/` — bundled Jackson (HTTP JSON) and jBCrypt; `lib/test/` contains JUnit dependencies.

## Common commands

- Build: `.\build.ps1`
- Run: `.\run.ps1` (the three demo accounts initialize automatically; Ctrl+C stops the local server)
- Test: `.\test.ps1`
- VS Code task palette: **PeopleDesk: Build**, **PeopleDesk: Run**, and **PeopleDesk: Test**.

The fixed demo usernames are `admin`, `hr-admin`, and `employee`. Their passwords are stored as BCrypt hashes; no startup password prompt or password environment variable is used. The Employee demo login is linked to `EMP001`. The 50 employee records are always loaded from `DemoSeed.java`.

Application changes last only until the server stops. Every restart restores the 50 predefined employees, six departments, and the three demo accounts. There is no application data folder, database, persistence, seed script, or account setup file.
