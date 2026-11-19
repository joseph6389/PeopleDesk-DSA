# Phase 5.1 — Visual Verification Report

Date: 31 October 2026  
Application URL: `http://127.0.0.1:8080/`  
Scope: browser verification of the existing PeopleDesk UI and supplemental localhost API checks. No application code or functionality was changed in this phase.

## Summary

**Overall: NOT VERIFIED for the requested visual/browser acceptance.** The application built and started successfully, and live HTTP checks confirmed the three demo roles and server-side authorization. The browser-control service could not establish the current Chrome URL and stopped the UI automation session under its safety policy. Consequently, browser interaction, console inspection, responsive layout checks and UI feature execution could not be completed. This report does not treat API responses or source inspection as a substitute for those browser checks.

No application defect was reproduced. No application source change or backup was necessary. The test server was stopped after checks.

## Preparation and project structure

- Reviewed `docs/CLEANUP-REVIEW-20261101.md`, `docs/SECURITY.md`, `README.md`, `run.ps1`, the web assets, and the Java test tree.
- Confirmed the current project still uses Java 17, the built-in loopback `HttpServer`, HTML/CSS/vanilla JavaScript, in-memory Java collections, BCrypt, and Jackson for HTTP JSON messages.
- Confirmed `run.ps1` checks the selected port, builds with Java 17, copies the web assets and launches the local server. No other launch script was needed.
- `run.ps1` started at `127.0.0.1:8080`. Startup output reported 50 employees and three demo accounts.
- The browser-control service first failed to open its browser tab and then explicitly stopped because it could not determine Chrome's current URL with sufficient confidence. The UI/browser checks below are therefore marked **NOT VERIFIED**.

## Admin browser results

| Check | Result | Notes |
|---|---|---|
| Login form interaction and successful Admin login | NOT VERIFIED | Could not interact with the browser UI. |
| Admin dashboard appearance and displayed role | NOT VERIFIED | API dashboard returned 50 employees; this does not verify visual rendering. |
| Employee list displays all 50 employees | NOT VERIFIED | API returned 50 employee records; table rendering/search controls were not exercised. |
| Six departments available | NOT VERIFIED | API returned six seeded departments; department screen was not opened. |
| Search, filtering, sorting, pagination | NOT VERIFIED | Not exercised in a browser. |
| Department and account management screens | NOT VERIFIED | Not opened in a browser. |
| Registration approvals, employee requests and activity history | NOT VERIFIED | Not opened in a browser. |
| Salary Calculator | NOT VERIFIED | No sample inputs or output were exercised in the UI. |
| DSA Laboratory operations | NOT VERIFIED | No UI operations were exercised. |
| Logout and protected-page behavior in browser | NOT VERIFIED | Session logout was checked through HTTP API only. |

## HR browser results

| Check | Result | Notes |
|---|---|---|
| HR login, dashboard appearance, displayed identity/role | NOT VERIFIED | Live API login returned role `HR`; browser rendering was not checked. |
| Employee and department management UI | NOT VERIFIED | Not opened in a browser. |
| Registration review and employee requests UI | NOT VERIFIED | Not opened in a browser. |
| Admin-only account operations in UI | NOT VERIFIED | Direct API authorization checks are recorded below. |
| Logout in browser | NOT VERIFIED | Logout was checked through HTTP API only. |

## Employee browser results

| Check | Result | Notes |
|---|---|---|
| Employee login and dashboard appearance | NOT VERIFIED | Live API login returned role `EMPLOYEE`; browser rendering was not checked. |
| Employee identity and personal details | NOT VERIFIED | API confirmed the account is linked to `EMP001` and the employee dashboard data is for `EMP001`. Personal detail rendering was not checked. |
| Own requests/account controls | NOT VERIFIED | API returned only the employee's own request collection; UI controls were not exercised. |
| Employee-wide management, other employee details, HR/Admin operations | NOT VERIFIED | Direct API authorization checks are recorded below. |
| Salary Calculator and DSA access | NOT VERIFIED | No UI testing was possible; current Employee navigation does not expose those sections. |
| Logout in browser | NOT VERIFIED | Logout was checked through HTTP API only. |

## Browser console, resources, and layouts

| Check | Result | Notes |
|---|---|---|
| JavaScript console errors | NOT VERIFIED | Browser developer tools were not accessible. |
| Missing CSS, JavaScript, favicon or other resource errors | NOT VERIFIED | Browser console/network panel was unavailable. The HTML, CSS, JavaScript and favicon endpoints each returned HTTP 200 when requested directly. |
| Broken navigation links, dialogs and form behavior | NOT VERIFIED | Not exercised in the browser. |
| Desktop layout | NOT VERIFIED | No visual browser inspection. |
| Tablet layout | NOT VERIFIED | No visual browser inspection. |
| Mobile layout and horizontal overflow | NOT VERIFIED | No visual browser inspection. |
| JavaScript syntax | PASS | `node --check src/main/webapp/assets/app.js` completed successfully. This does not check runtime browser errors. |

## Salary Calculator

**NOT VERIFIED.** The calculator was not opened or exercised. Sample input validation, monthly/annual outputs, formatting, error behavior, and whether calculation leaves employee salary records unchanged were not tested in a browser during this phase. No tax or statutory deductions were added.

## Live localhost authentication and authorization checks

These are API checks, not browser checks.

| Check | Result | Evidence |
|---|---|---|
| Admin credentials | PASS | Login returned `ADMIN`. Dashboard reported 50 employees. |
| HR credentials | PASS | Login returned `HR`. Dashboard reported 50 employees. |
| Employee credentials | PASS | Login returned `EMPLOYEE`, linked to `EMP001`; employee dashboard record was `EMP001`. |
| Unknown username / wrong credentials | PASS | Rejected with HTTP 401. |
| Seeded employee/departments | PASS | Employee API returned 50 records; HR department API returned six departments. |
| HR registration queue and requests access | PASS | HR registration-request API was reachable; Employee request API response contained no records belonging to another employee. |
| HR account visibility | PASS | HR account list contained Employee-role entries only. |
| HR Admin account provisioning | PASS | Direct POST attempt returned HTTP 403. |
| HR Admin account deactivation | PASS | Direct attempt to deactivate Admin returned HTTP 403. |
| Employee employee-wide list | PASS | Direct request returned HTTP 403. |
| Employee other employee record (`EMP002`) | PASS | Direct request returned HTTP 403. |
| Employee DSA endpoint | PASS | Direct request returned HTTP 403. |
| Login page, CSS, JS and favicon HTTP responses | PASS | Each requested resource returned HTTP 200. |
| Logout/session invalidation | PASS | Logout returned HTTP 200; subsequent `/api/me` returned HTTP 401. |

The UI-only salary, DSA, department, account, registration, request and activity interactions remain unverified. No direct API check was interpreted as proof that a screen works.

## Build and automated tests

- Java 17 build through `run.ps1`: **PASS**.
- Java 17 build/test script through `test.ps1`: **PASS**.
- Automated tests: **26 passed, 0 failed**.
- JavaScript syntax check: **PASS**.
- Browser tests: **NOT VERIFIED** because the browser-control session stopped before UI access.
- Server startup and stop: **PASS**; server started on the requested localhost URL and was stopped after checks.

## Defects, changes, backup, and remaining work

- Defects reproduced: **none**.
- Fixes applied: **none**.
- Application files changed: **none**.
- Project files deleted: **none**.
- Backup: **not applicable**; no files were modified or deleted.
- Outstanding: perform the Admin, HR and Employee UI workflows in a browser where the browser-control session can attach reliably; inspect the browser console; exercise the Salary Calculator and DSA Laboratory; and check desktop, tablet and mobile layouts. Until then, the requested visual/browser acceptance remains **NOT VERIFIED**.
