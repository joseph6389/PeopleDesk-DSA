package edu.college.employee.web;

import edu.college.employee.model.RegistrationRequest;
import edu.college.employee.service.App;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.mindrot.jbcrypt.BCrypt;

/** Public employee registration; the employee chooses no role and must match an existing record. */
public final class RegistrationController {
    public void post(HttpRequest request, HttpResponse response) throws IOException {
        try {
            var body = Web.body(request);
            String name = Web.str(body, "name").strip();
            String employeeId = Web.str(body, "employeeId").strip();
            String email = Web.str(body, "email").strip();
            String phone = Web.str(body, "phone").strip();
            String username = Web.str(body, "username").strip();
            String password = Web.str(body, "password");
            String confirm = Web.str(body, "confirmPassword");
            if (name.length() > 100 || employeeId.length() > 32 || email.length() > 254 || phone.length() > 32
                    || !username.matches("[A-Za-z0-9._-]{3,80}"))
                throw new IllegalArgumentException("Please check the registration details.");
            if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
                throw new IllegalArgumentException("Password must be at least 12 characters and at most 72 UTF-8 bytes.");
            if (password.chars().noneMatch(Character::isLowerCase) || password.chars().noneMatch(Character::isUpperCase)
                    || password.chars().noneMatch(Character::isDigit) || password.chars().allMatch(Character::isLetterOrDigit))
                throw new IllegalArgumentException("Use upper and lowercase letters, a number, and a symbol in the password.");
            if (!password.equals(confirm)) throw new IllegalArgumentException("Passwords do not match.");
            var repo = App.repo();
            var employee = repo.employees().stream().filter(e -> e.id().equalsIgnoreCase(employeeId)
                    && e.name().equalsIgnoreCase(name) && e.email().equalsIgnoreCase(email)
                    && e.phone().equals(phone)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Employee details could not be matched. Contact HR for help."));
            if (!employee.status().equals("ACTIVE"))
                throw new IllegalArgumentException("This employee record is not active. Contact HR for help.");
            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            if (repo.users().stream().anyMatch(u -> u.username().equalsIgnoreCase(username)))
                throw new IllegalArgumentException("That username is already in use.");
            if (repo.users().stream().anyMatch(u -> "EMPLOYEE".equals(u.role())
                    && employee.id().equalsIgnoreCase(u.employeeId())))
                throw new IllegalArgumentException("This employee already has an account.");
            if (repo.users().stream().filter(u -> "EMPLOYEE".equals(u.role())).anyMatch(u -> repo.employees().stream()
                    .anyMatch(e -> e.id().equalsIgnoreCase(u.employeeId()) && e.email().equalsIgnoreCase(email))))
                throw new IllegalArgumentException("That email is already linked to an account.");
            if (repo.registrationRequests().stream().anyMatch(r -> r.status().equals("PENDING")
                    && r.email().equalsIgnoreCase(email)))
                throw new IllegalArgumentException("A registration request already uses that email.");
            repo.changeRegistrationRequests(requests -> {
                String normalized = username.toLowerCase(Locale.ROOT);
                if (requests.stream().anyMatch(r -> r.username().toLowerCase(Locale.ROOT).equals(normalized)
                        && r.status().equals("PENDING")))
                    throw new IllegalArgumentException("A request already uses that username.");
                if (requests.stream().anyMatch(r -> r.employeeId().equalsIgnoreCase(employee.id())
                        && r.status().equals("PENDING")))
                    throw new IllegalArgumentException("A registration request for this employee is already pending.");
                requests.add(0, new RegistrationRequest(java.util.UUID.randomUUID().toString(), username, hash,
                        employee.id(), name, email, phone, "PENDING", java.time.Instant.now(), null, null));
                return null;
            });
            App.log(repo, username, "EMPLOYEE_REGISTRATION_SUBMITTED", employee.id());
            Web.send(response, 202, Map.of("message", "Your request was sent to HR. You can sign in after it is approved."));
        } catch (IllegalArgumentException e) {
            Web.send(response, 400, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            Web.send(response, 500, Map.of("error", "Registration could not be completed."));
        }
    }
}
