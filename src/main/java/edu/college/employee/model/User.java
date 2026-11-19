package edu.college.employee.model;

public record User(String username, String passwordHash, String role, String employeeId, Boolean active) {
    public User(String username, String passwordHash, String role, String employeeId) {
        this(username, passwordHash, role, employeeId, true);
    }

    public User {
        if (active == null) active = true;
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,80}")
                || passwordHash == null || passwordHash.isBlank()
                || (!"ADMIN".equals(role) && !"HR".equals(role) && !"EMPLOYEE".equals(role))) {
            throw new IllegalArgumentException("Invalid user");
        }
        if ("EMPLOYEE".equals(role) && (employeeId == null || employeeId.isBlank())) {
            throw new IllegalArgumentException("Employee account requires employee ID");
        }
        if (!"EMPLOYEE".equals(role) && employeeId != null) {
            throw new IllegalArgumentException("Privileged account cannot be linked to an employee record");
        }
    }
}
