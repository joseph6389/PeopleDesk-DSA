package edu.college.employee.web;

import edu.college.employee.model.User;
import edu.college.employee.service.App;
import edu.college.employee.service.AppData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.mindrot.jbcrypt.BCrypt;

public final class AuthController {
    private static final int MAX_FAILED_LOGINS = 5;
    private static final long LOGIN_WINDOW_MILLIS = 60_000;
    private static final Map<String, LoginAttempts> LOGIN_ATTEMPTS = new ConcurrentHashMap<>();
    private static final class LoginAttempts { long windowStart; int failures; long blockedUntil; }

    static boolean loginBlocked(String username, long now) {
        LoginAttempts attempts = LOGIN_ATTEMPTS.get(username.toLowerCase(java.util.Locale.ROOT));
        if (attempts == null) return false;
        synchronized (attempts) {
            if (now >= attempts.blockedUntil) {
                if (now - attempts.windowStart >= LOGIN_WINDOW_MILLIS || attempts.blockedUntil > 0) {
                    LOGIN_ATTEMPTS.remove(username.toLowerCase(java.util.Locale.ROOT), attempts);
                    return false;
                }
                return false;
            }
            return true;
        }
    }

    private static void failedLogin(String username, long now) {
        String key = username.toLowerCase(java.util.Locale.ROOT);
        LoginAttempts attempts = LOGIN_ATTEMPTS.computeIfAbsent(key, ignored -> new LoginAttempts());
        synchronized (attempts) {
            if (now - attempts.windowStart >= LOGIN_WINDOW_MILLIS) { attempts.windowStart = now; attempts.failures = 0; }
            if (++attempts.failures >= MAX_FAILED_LOGINS) attempts.blockedUntil = now + LOGIN_WINDOW_MILLIS;
        }
    }

    static void recordFailedLoginForTest(String username, long now) { failedLogin(username, now); }

    private static void successfulLogin(String username) {
        LOGIN_ATTEMPTS.remove(username.toLowerCase(java.util.Locale.ROOT));
    }

    static void clearLoginAttemptsForTest() { LOGIN_ATTEMPTS.clear(); }
    public void get(HttpRequest request, HttpResponse response) throws IOException {
        Web.UserSession user = Web.user(request);
        if (user == null) {
            Web.send(response, 401, Map.of("error", "Authentication required"));
            return;
        }
        Object token = request.getSession(false).getAttribute("csrf");
        Web.send(response, 200, Map.of(
                "username", user.username(),
                "role", user.role(),
                "employeeId", user.employeeId() == null ? "" : user.employeeId(),
                "csrf", token == null ? "" : token));
    }

    public void post(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getRequestURI();
        if (path.endsWith("logout")) {
            LocalSession existing = request.getSession(false);
            if (existing != null) existing.invalidate();
            response.setHeader("Set-Cookie", "EMPLOYEE_SESSION=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
            Web.send(response, 200, Map.of("ok", true));
            return;
        }
        if (!path.endsWith("login")) {
            Web.send(response, 405, Map.of("error", "Method not allowed"));
            return;
        }
        try {
            var body = Web.body(request);
            String username = Web.str(body, "username");
            String password = Web.str(body, "password");
            if (username.length() > 80 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("Invalid credentials");
            }
            long now = System.currentTimeMillis();
            if (loginBlocked(username, now)) {
                Web.send(response, 429, Map.of("error", "Too many failed sign-in attempts. Try again in one minute."));
                return;
            }
            AppData repositories = App.repo();
            User found = repositories.users().stream()
                    .filter(account -> account.username().equalsIgnoreCase(username)
                            || ("EMPLOYEE".equals(account.role()) && repositories.employees().stream()
                                .anyMatch(employee -> employee.id().equals(account.employeeId()) && employee.email().equalsIgnoreCase(username))))
                    .findFirst().orElse(null);
            boolean passwordMatches = false;
            if (found != null) {
                try {
                    passwordMatches = found.active() && BCrypt.checkpw(password, found.passwordHash());
                } catch (IllegalArgumentException ignored) {
                    // Malformed stored hashes must fail as an ordinary invalid login.
                }
            }
            if (!passwordMatches) {
                failedLogin(username, now);
                App.log(repositories, username, "LOGIN_FAILURE", "account");
                Web.send(response, 401, Map.of("error", "Invalid username or password"));
                return;
            }
            successfulLogin(username);

            LocalSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute("user", new Web.UserSession(found.username(), found.role(), found.employeeId()));
            String csrf = UUID.randomUUID().toString();
            session.setAttribute("csrf", csrf);
            session.setMaxInactiveInterval(1800);
            App.log(repositories, found.username(), "LOGIN_SUCCESS", "account");
            request.writeSessionCookie(response);
            Web.send(response, 200, Map.of(
                    "username", found.username(),
                    "role", found.role(),
                    "employeeId", found.employeeId() == null ? "" : found.employeeId(),
                    "csrf", csrf));
        } catch (IllegalArgumentException e) {
            Web.send(response, 400, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            Web.send(response, 500, Map.of("error", "Unable to authenticate"));
        }
    }
}
