package edu.college.employee.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.Instant;

/** Employee request with optional ISO date range for leave requests. */
public record EmployeeRequest(String id, String employeeId, String type, String details, String status,
                              String createdBy, Instant createdAt, String decidedBy, Instant decidedAt,
                              String decisionNote, String leaveFrom, String leaveTo) {
    public EmployeeRequest {
        if (id == null || employeeId == null || type == null || details == null || status == null
                || createdBy == null || createdAt == null) {
            throw new IllegalArgumentException("Invalid request");
        }
        if (type.equalsIgnoreCase("Leave")) {
            if (leaveFrom == null || leaveFrom.isBlank() || leaveTo == null || leaveTo.isBlank()) {
                throw new IllegalArgumentException("Leave From and Leave To dates are required.");
            }
            try {
                LocalDate from = LocalDate.parse(leaveFrom);
                LocalDate to = LocalDate.parse(leaveTo);
                if (to.isBefore(from)) {
                    throw new IllegalArgumentException("Leave To cannot be earlier than Leave From.");
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Leave dates must use YYYY-MM-DD format.");
            }
        }
    }
}
