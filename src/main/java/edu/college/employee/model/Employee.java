package edu.college.employee.model;

import java.time.LocalDate;
import java.math.BigDecimal;

public record Employee(String id, String name, String department, String designation,
                       String email, String phone, LocalDate joiningDate, String status, BigDecimal salary) {
    public Employee(String id, String name, String department, String designation, String email,
                    String phone, LocalDate joiningDate, String status) {
        this(id, name, department, designation, email, phone, joiningDate, status, BigDecimal.ZERO);
    }

    public Employee {
        id = required(id, "id", 32); name = required(name, "name", 100);
        department = required(department, "department", 80); designation = required(designation, "designation", 80);
        email = required(email, "email", 254); phone = required(phone, "phone", 32);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Invalid email address");
        if (joiningDate == null) throw new IllegalArgumentException("Joining date is required");
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"ON_LEAVE".equals(status)) throw new IllegalArgumentException("Invalid employment status");
        if (salary == null || salary.signum() < 0 || salary.scale() > 2) throw new IllegalArgumentException("Invalid salary");
        salary = salary.setScale(2);
    }
    private static String required(String value, String field, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("Invalid " + field);
        return value.strip();
    }
}
