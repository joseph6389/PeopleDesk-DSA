# PeopleDesk progress

## In-memory storage conversion

- Replaced JSON file storage with `service/AppData.java`, which owns Java `ArrayList` collections for employees, accounts, departments, registration requests, employee requests, and activity history.
- `service/DemoSeed.java` defines exactly 50 fictional employee records (EMP001–EMP050), each with a unique email and salary. Every new server process loads these records.
- Admin and HR passwords are collected at launch, BCrypt hashed, and stored only as hashes in the in-memory account list. Public registration remains Employee-only and requires HR/Admin approval.
- Removed `JsonStore`, file-based `Repositories`, the local setup/seed scripts, and the old active employee JSON files after creating backups.
- Jackson remains for JSON HTTP request and response encoding only. Application code does not read or write JSON data files.
- Runtime changes are temporary. Restarting restores the 50 predefined employees and launch-configured Admin/HR accounts; added accounts, departments, registrations, requests, and activity are cleared.

## Backups

- Project backup: `backup-before-memory-storage-20261030-133541.zip`
- Old LocalAppData records archive: `C:\Users\josep\AppData\Local\EmployeeRecordData.backup-before-memory-storage-20261030-133541.zip`

## Verification

- **Build: PASS** — Java 17.
- **Automated tests: PASS** — 20/20 JUnit tests; browser JavaScript syntax check passed.
- **API: PASS** — Admin/HR/Employee login, invalid credentials, employee registration and approval, employee linking and ownership, employee CRUD, department CRUD, requests/approval, DSA search/sort/tree/allocation, and activity history.
- **Browser: PASS** — Admin dashboard showed 50 employees and six departments. Salary calculator returned ₹55,000 gross and ₹53,000 net for the sample. Employee details showed the seeded salary. Browser console had no errors.
- **Restart: PASS** — stopped and relaunched the application; it reloaded EMP001–EMP050, 50 employees total, and cleared runtime requests and test employee changes.
- **File storage scan: PASS** — removed active employee/account/request/department/activity JSON files and Java JSON persistence. JSON remains only for HTTP messages and editor configuration.
