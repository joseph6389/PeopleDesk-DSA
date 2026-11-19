package edu.college.employee.model;

import java.time.Instant;

/** A registration request holds only a BCrypt hash until an administrator approves it. */
public record RegistrationRequest(String id, String username, String passwordHash, String employeeId,
                                  String name, String email, String phone, String status,
                                  Instant createdAt, String reviewedBy, Instant reviewedAt) {
    public RegistrationRequest {
        if (id == null || username == null || passwordHash == null || employeeId == null
                || name == null || email == null || phone == null || status == null || createdAt == null)
            throw new IllegalArgumentException("Invalid registration request");
        if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED"))
            throw new IllegalArgumentException("Invalid registration request status");
    }
}
