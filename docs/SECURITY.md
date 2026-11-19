# Security notes

## Implemented

- Admin, HR, and Employee roles are checked on the server. Public registration creates only a pending Employee request linked to an active predefined employee; HR/Admin approval activates the account.
- Login, registration, and account provisioning use BCrypt password hashes. Each start initializes the fixed `admin`, `hr-admin`, and `employee` demo accounts from BCrypt hashes. The Employee demo account is linked to active seeded record `EMP001`; it does not need registration approval. The application never stores demo passwords as plain text in user records.
- Session IDs are unpredictable and server-side, rotate at login and password change, expire after 30 minutes, and use HttpOnly/SameSite cookies. Deactivation invalidates the account's sessions.
- Authenticated state-changing routes require a CSRF token. Role and employee ownership checks run in Java, including direct API requests.
- Login attempts are throttled by username after five failures in a one-minute window; the block lasts one minute. Throttle state is held in memory and resets when the server restarts.
- Employees can view only their linked record and requests. Their profile endpoint accepts only name, email, and phone; HR/Admin control employment data.
- Admin provisions HR accounts. HR cannot view or modify Admin/HR accounts. HR/Admin can provision linked employee accounts and manage employee activation.
- The built-in HTTP server binds to loopback. All application data exists only in Java collections in the current process. Jackson is used for HTTP request/response encoding, not file storage.

## Runtime credentials and data lifetime

The local demonstration accounts are initialized automatically by `AppData`; `run.ps1` does not prompt for passwords and no Admin/HR password environment variables are used. The demo credentials are intended only for the local college demonstration. Do not expose the server or reuse these demo accounts for real personnel data.

All state is lost when the server stops. A restart reloads the 50 fictional employees, six departments, and three demo accounts. Other runtime-created accounts and subsequent changes must be recreated during each run.

## Known limits

This is a localhost college project, not a production deployment. It has no HTTPS listener, MFA, or independent penetration test. Do not expose it to a network. Demo password values are not printed or logged; login activity records the account name and outcome only.
