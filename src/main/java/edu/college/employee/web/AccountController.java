package edu.college.employee.web;

import edu.college.employee.model.User;
import edu.college.employee.service.App;
import edu.college.employee.service.AppData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.mindrot.jbcrypt.BCrypt;

/** HR-only account onboarding; no public account registration route exists. */
public final class AccountController {
    public void handle(HttpRequest request, HttpResponse response) throws IOException {
        Web.UserSession actor = Web.user(request);
        if (actor == null || !(actor.role().equals("HR") || actor.role().equals("ADMIN"))) {
            Web.send(response, 403, Map.of("error", "Forbidden"));
            return;
        }
        AppData repositories = App.repo();
        if (request.getMethod().equals("GET") && request.getRequestURI().equals("/api/accounts")) {
            var accounts = repositories.users().stream()
                    .filter(user -> actor.role().equals("ADMIN") || user.role().equals("EMPLOYEE"))
                    .map(user -> Map.of(
                            "username", user.username(),
                            "role", user.role(),
                            "employeeId", user.employeeId() == null ? "" : user.employeeId(),
                            "active", user.active()))
                    .toList();
            Web.send(response, 200, accounts);
            return;
        }
        if (request.getMethod().equals("POST") && request.getRequestURI().equals("/api/accounts/activation")) {
            try {
                var body = Web.body(request);
                String username = Web.str(body, "username");
                if (body.get("active") == null || !body.get("active").isBoolean())
                    throw new IllegalArgumentException("Choose whether the account should be active.");
                boolean active = body.get("active").asBoolean();
                var target = repositories.users().stream().filter(user -> user.username().equalsIgnoreCase(username))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Account not found."));
                if (actor.role().equals("HR") && !target.role().equals("EMPLOYEE")) {
                    Web.send(response, 403, Map.of("error", "Forbidden"));
                    return;
                }
                if (!active && target.username().equalsIgnoreCase(actor.username()))
                    throw new IllegalArgumentException("You cannot deactivate your own account.");
                if (!active && target.role().equals("ADMIN") && repositories.users().stream()
                        .filter(user -> user.role().equals("ADMIN") && user.active()).count() <= 1)
                    throw new IllegalArgumentException("The last active Admin account cannot be deactivated.");
                repositories.changeUsers(users -> {
                    for (int i = 0; i < users.size(); i++) if (users.get(i).username().equalsIgnoreCase(username)) {
                        User current = users.get(i);
                        users.set(i, new User(current.username(), current.passwordHash(), current.role(), current.employeeId(), active));
                        return null;
                    }
                    throw new IllegalArgumentException("Account not found.");
                });
                if (!active) LocalSession.invalidateOtherSessions(username, null);
                App.log(repositories, actor.username(), active ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED", username);
                Web.send(response, 200, Map.of("username", username, "active", active));
            } catch (IllegalArgumentException e) {
                Web.send(response, 400, Map.of("error", e.getMessage()));
            }
            return;
        }
        if (request.getMethod().equals("POST") && request.getRequestURI().equals("/api/accounts/hr")) {
            if (!actor.role().equals("ADMIN")) {
                Web.send(response, 403, Map.of("error", "Forbidden"));
                return;
            }
            try {
                var body = Web.body(request);
                String username = Web.str(body, "username");
                String password = Web.str(body, "password");
                if (!username.matches("[A-Za-z0-9._-]{3,80}")) throw new IllegalArgumentException("Invalid username.");
                validatePassword(password);
                String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                repositories.changeUsers(users -> {
                    if (users.stream().anyMatch(user -> user.username().equalsIgnoreCase(username)))
                        throw new IllegalArgumentException("Username already exists.");
                    users.add(new User(username, hash, "HR", null));
                    return null;
                });
                App.log(repositories, actor.username(), "HR_ACCOUNT_CREATED", username);
                Web.send(response, 201, Map.of("username", username, "role", "HR"));
            } catch (IllegalArgumentException e) {
                Web.send(response, 400, Map.of("error", e.getMessage()));
            }
            return;
        }
        if (!request.getMethod().equals("POST") || !request.getRequestURI().equals("/api/accounts/employee")) {
            Web.send(response, 405, Map.of("error", "Method not allowed"));
            return;
        }
        try {
            var body = Web.body(request);
            String username = Web.str(body, "username");
            String employeeId = Web.str(body, "employeeId");
            String password = Web.str(body, "password");
            if (!username.matches("[A-Za-z0-9._-]{3,80}"))
                throw new IllegalArgumentException("Username must use 3-80 letters, digits, dots, underscores, or hyphens");
            validatePassword(password);
            var employee = repositories.employees().stream()
                    .filter(record -> record.id().equalsIgnoreCase(employeeId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Employee record not found"));
            if (!employee.status().equals("ACTIVE"))
                throw new IllegalArgumentException("An account can only be created for an active employee.");
            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            repositories.changeUsers(users -> {
                String normalized = username.toLowerCase(Locale.ROOT);
                if (users.stream().anyMatch(user -> user.username().toLowerCase(Locale.ROOT).equals(normalized)))
                    throw new IllegalArgumentException("Username already exists");
                if (users.stream().anyMatch(user -> user.role().equals("EMPLOYEE")
                        && user.employeeId().equalsIgnoreCase(employee.id())))
                    throw new IllegalArgumentException("This employee already has a linked login");
                users.add(new User(username, hash, "EMPLOYEE", employee.id()));
                return null;
            });
            App.log(repositories, actor.username(), "EMPLOYEE_ACCOUNT_CREATED", employee.id());
            Web.send(response, 201, Map.of("username", username, "employeeId", employee.id(), "role", "EMPLOYEE"));
        } catch (IllegalArgumentException e) {
            Web.send(response, 400, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            Web.send(response, 500, Map.of("error", "Account could not be created"));
        }
    }

    private static void validatePassword(String password) {
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().allMatch(Character::isLetterOrDigit))
            throw new IllegalArgumentException("Use 12+ characters with upper and lowercase letters, a number, and a symbol.");
    }
}
