# PeopleDesk project file review

Review date: 1 November 2026  
Scope: current workspace file tree and references in Java, web assets, build/run/test scripts, IDE settings, README, project map, and documentation. The initial inspection and classification were completed before any file was deleted.

## Classification

| Classification | Files / folders | Evidence and action |
|---|---|---|
| REQUIRED | `src/main/java/**` | Application entry point, server, controllers, models, in-memory state, seed data, and DSA endpoints reference these classes. Keep. |
| REQUIRED | `src/main/webapp/index.html`, `assets/app.js`, `assets/app.css` | `index.html` references the CSS, JS, and favicon. The JS implements the active application flows; the stylesheet provides the existing theme/layout. Keep. |
| REQUIRED | `src/main/webapp/assets/favicon.svg` | Explicitly referenced by `index.html`. Keep. |
| REQUIRED | `src/test/java/**`, `scripts/TestRunner.java`, `test.ps1` | Test runner compiles and executes the JUnit suite. Existing tests protect data, DSA, request parsing, and login throttling. Keep and extend for the demo-account behavior. |
| REQUIRED | `build.ps1`, `run.ps1` | The run script invokes the build, copies web assets through the build, and launches Java 17. Keep; update only obsolete credential prompts. |
| REQUIRED | `lib/**` | Build and runtime classpaths use Jackson, jBCrypt, and JUnit jars. Keep. |
| REQUIRED | `.classpath`, `.project`, `.vscode/settings.json`, `.vscode/tasks.json` | Eclipse/VS Code project and task configuration. These are IDE configuration JSON files, not PeopleDesk data storage. Keep. |
| REQUIRED | `README.md`, `PROJECT_MAP.md`, `docs/SECURITY.md`, `docs/DSA-IMPLEMENTATION.md`, `docs/PROGRESS.md`, `docs/FINAL-AUDIT-20261030.md`, `docs/BASELINE-2026-10-30.md` | Current user/developer and audit documentation. Update active setup guidance; preserve dated audit records as historical evidence. Keep. |
| REQUIRED | `project-documentation/**` | User-requested report, PDF, presentation, README, and diagrams. Explicitly protected by the request. Keep. |
| OPTIONAL | `build/**` | Generated compiled classes and copied web assets. `run.ps1` rebuilds them; IDE tasks may also use generated output. Retain for current workspace convenience; the application can regenerate them. |
| OPTIONAL | `.documentation-work/**` | Intermediate sources and render checks from producing the protected documentation package. Not used by the application. Retain to avoid removing useful documentation provenance. |
| OPTIONAL / UNCERTAIN | `audit-backup-20261030/**` | Older audit/documentation snapshots. Not loaded by the application; retained as historical backup material. |
| UNCERTAIN — KEEP | `backup-before-*.zip` | Earlier project snapshots. No runtime/build references, but their purpose is recovery; do not delete. A new complete pre-change backup will also be created. |
| CONFIRMED UNUSED — REMOVED AFTER BACKUP | `.integration-data/server-final.err.log`, `.integration-data/server-final.out.log` | Stale generated integration-run logs. No Java, frontend, build/run/test script, README, project-map, or documentation reference requires these paths. They are not runtime inputs. Both were removed after the complete backup; the empty `.integration-data` directory remains. |
| UNCERTAIN — KEEP | Other hidden/generated files not listed above | No further safe-to-delete candidates were confirmed. Keep until their purpose can be established. |

## Specific checks

- No PeopleDesk employee/account JSON persistence file is present in the current source tree. The only `.json` files found are VS Code settings/tasks. Jackson references are for HTTP request/response messages.
- There is one active JavaScript file and one active CSS file; both are referenced by `index.html`.
- The favicon is referenced. No other application image assets were found outside the protected documentation diagrams.
- No separate obsolete `setup-admin` script is present. The obsolete setup flow currently consists of launch-time environment/hidden-password handling, the `/api/setup-status` route, and its login-screen message; these references are to be removed as part of the credential change.
- `docs/BASELINE-2026-10-30.md` and `docs/FINAL-AUDIT-20261030.md` contain dated historical observations. Their old setup/account descriptions are not current startup instructions and will remain unchanged as audit records.
- No broken application asset reference was found: the page points to existing `assets/app.css`, `assets/app.js`, and `assets/favicon.svg`.

## Cleanup decision

The classification and deletion decision were recorded before cleanup. A complete backup was created at `backup-before-demo-login-20261101.zip` before removing the two confirmed stale integration logs. The archive contains 197 entries and was verified. No other project files were deleted. Historical backups, generated build outputs, test files, IDE configuration, the documentation package, and audit reports were retained.

## Completion record

- Removed: `.integration-data/server-final.err.log` and `.integration-data/server-final.out.log` only.
- Updated as part of the authorized credential change: startup account documentation and launcher references; obsolete environment-password and setup-status flow was removed from active application source.
- Preserved: all Java source, frontend assets, tests, libraries, build/run scripts, documentation package, dated audit records, historical backups, and uncertain/optional files.
- Verification: Java 17 build succeeded; all 26 automated tests passed; `node --check src/main/webapp/assets/app.js` passed; live localhost checks confirmed all three roles authenticate, employee/HR forbidden routes are denied, logout invalidates a session, and restart restores 50 employees, six departments, and three demo accounts while discarding a temporary runtime department.
