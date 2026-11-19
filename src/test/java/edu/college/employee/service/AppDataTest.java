package edu.college.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.college.employee.model.Activity;
import edu.college.employee.model.EmployeeRequest;
import edu.college.employee.model.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.mindrot.jbcrypt.BCrypt;
import org.junit.jupiter.api.Test;

class AppDataTest {
    @Test
    void loadsAllMigratedEmployeesAndPersistsChanges() {
        AppData data = edu.college.employee.service.TestAppData.create();
        int originalCount = data.employees().size();
        assertEquals(52, originalCount);
        assertEquals(originalCount, data.employees().stream().map(e -> e.id().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet()).size());
        assertEquals(originalCount, data.employees().stream().map(e -> e.email().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet()).size());
        assertTrue(data.searchEmployeeInList("emp052") != null);
        assertTrue(data.searchEmployeeInTree("EMP052") != null);
        assertEquals("EMP031", data.employees().get(0).id());
        assertEquals("EMP015", data.employees().get(1).id());
        assertTrue(data.employees().stream().allMatch(e -> e.salary().compareTo(BigDecimal.ZERO) > 0));
        data.changeEmployees(employees -> { employees.remove(0); return null; });
        assertEquals(originalCount - 1, data.employees().size());
        assertEquals(originalCount, edu.college.employee.service.TestAppData.create().employees().size());
    }

    @Test
    void seedsExactlyThreeHashedDemoAccountsAndRestoresThemForANewRun() {
        AppData data = edu.college.employee.service.TestAppData.create();
        assertEquals(3, data.users().size());
        assertTrue(data.users().stream().anyMatch(user -> user.username().equals("admin")
                && user.role().equals("ADMIN") && user.employeeId() == null
                && user.passwordHash().startsWith("$2a$") && BCrypt.checkpw("admin1234", user.passwordHash())));
        assertTrue(data.users().stream().anyMatch(user -> user.username().equals("hr-admin")
                && user.role().equals("HR") && user.employeeId() == null
                && user.passwordHash().startsWith("$2a$") && BCrypt.checkpw("hradmin1234", user.passwordHash())));
        assertTrue(data.users().stream().anyMatch(user -> user.username().equals("employee")
                && user.role().equals("EMPLOYEE") && user.employeeId().equals("EMP001")
                && user.passwordHash().startsWith("$2a$") && BCrypt.checkpw("employee1234", user.passwordHash())));
        assertEquals(3, edu.college.employee.service.TestAppData.create().users().size());
        assertEquals("ACTIVE", data.employees().stream().filter(e -> e.id().equals("EMP001")).findFirst().orElseThrow().status());
        assertThrows(IllegalArgumentException.class, () -> data.changeUsers(users -> {
            users.add(new User("ADMIN", "duplicate", "HR", null));
            return null;
        }));
        assertEquals(3, data.users().size());
    }

    @Test
    void managesDepartmentsRequestsAndActivityInMemory() {
        AppData data = edu.college.employee.service.TestAppData.create();
        data.changeDepartments(departments -> { departments.add("Research"); return null; });
        assertTrue(data.departments().contains("Research"));
        EmployeeRequest request = new EmployeeRequest("R1", "EMP001", "Leave", "Personal leave",
                "PENDING", "employee-one", Instant.now(), null, null, null, "2026-11-02", "2026-11-04");
        data.submitRequest(request);
        data.changeActivity(entries -> { entries.add(new Activity("A1", Instant.now(), "employee-one", "REQUEST_SUBMITTED", "R1")); return null; });
        assertEquals("R1", data.requests().get(0).id());
        assertEquals("2026-11-02", data.requests().get(0).leaveFrom());
        assertEquals("A1", data.activity().get(0).id());
        assertThrows(IllegalStateException.class, () -> data.deleteEmployeeIfUnreferenced("EMP001"));
    }

    @Test
    void employeeRequestsAreKeptInFifoSubmissionOrder() {
        AppData data = edu.college.employee.service.TestAppData.create();
        Instant firstSubmitted = Instant.parse("2026-10-31T10:00:00Z");
        Instant secondSubmitted = Instant.parse("2026-10-31T10:01:00Z");
        data.submitRequest(new EmployeeRequest("R1", "EMP001", "General", "First request",
                "PENDING", "employee", firstSubmitted, null, null, null, null, null));
        data.submitRequest(new EmployeeRequest("R2", "EMP001", "General", "Second request",
                "PENDING", "employee", secondSubmitted, null, null, null, null, null));

        assertEquals(List.of("R1", "R2"), data.requests().stream().map(EmployeeRequest::id).toList());
    }

    @Test
    void leaveRequestsRequireValidInclusiveDateRanges() {
        assertThrows(IllegalArgumentException.class, () -> new EmployeeRequest("R1", "EMP001", "Leave", "Personal",
                "PENDING", "employee", Instant.now(), null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new EmployeeRequest("R2", "EMP001", "Leave", "Personal",
                "PENDING", "employee", Instant.now(), null, null, null, "2026-11-05", "2026-11-04"));
        assertThrows(IllegalArgumentException.class, () -> new EmployeeRequest("R3", "EMP001", "Leave", "Personal",
                "PENDING", "employee", Instant.now(), null, null, null, "not-a-date", "2026-11-06"));
        EmployeeRequest sameDay = new EmployeeRequest("R4", "EMP001", "Leave", "One day",
                "PENDING", "employee", Instant.now(), null, null, null, "2026-11-04", "2026-11-04");
        assertEquals("2026-11-04", sameDay.leaveTo());
    }

    @Test
    void activityStackDisplaysNewestFirstAndNewApplicationActivitiesArePushed() {
        AppData data = edu.college.employee.service.TestAppData.create();
        Activity older = new Activity("A1", Instant.parse("2026-11-02T10:00:00Z"), "hr", "LOGIN_SUCCESS", "account");
        Activity newer = new Activity("A2", Instant.parse("2026-11-02T10:01:00Z"), "hr", "EMPLOYEE_UPDATED", "EMP001");

        assertTrue(data.activityIsEmpty());
        assertEquals(0, data.activityCount());
        data.recordActivity(older);
        data.recordActivity(newer);

        assertFalse(data.activityIsEmpty());
        assertEquals(2, data.activityCount());
        assertEquals(List.of(newer, older), data.activity());

        App.log(data, "hr-admin", "REQUEST_APPROVED", "REQ-1");
        Activity generated = data.activity().get(0);
        assertEquals("REQUEST_APPROVED", generated.action());
        assertEquals("hr-admin", generated.actor());
        assertEquals(List.of(generated, newer, older), data.activity());
    }

    @Test
    void exposesReadOnlySnapshotsAndRollsBackInvalidCollectionChanges() {
        AppData data = edu.college.employee.service.TestAppData.create();
        int originalCount = data.employees().size();
        assertThrows(UnsupportedOperationException.class, () -> data.employees().clear());
        assertThrows(IllegalArgumentException.class, () -> data.changeEmployees(employees -> {
            employees.add(employees.get(0));
            return null;
        }));
        assertEquals(originalCount, data.employees().size());
        assertEquals(originalCount, new HashSet<>(data.employees().stream()
                .map(e -> e.id().toLowerCase(Locale.ROOT)).toList()).size());
    }

    @Test
    void rejectsDuplicateEmployeeEmailsWithoutChangingExistingRecords() {
        AppData data = edu.college.employee.service.TestAppData.create();
        int originalCount = data.employees().size();
        var original = data.employees().get(0);
        assertThrows(IllegalArgumentException.class, () -> data.changeEmployees(employees -> {
            var duplicateEmail = new edu.college.employee.model.Employee("NEW001", "New Person", "Engineering",
                    "Analyst", original.email().toUpperCase(Locale.ROOT), "+91-90000-00000",
                    original.joiningDate(), "ACTIVE", BigDecimal.ZERO);
            employees.add(duplicateEmail);
            return null;
        }));
        assertEquals(originalCount, data.employees().size());
        assertEquals(original.email(), data.employees().get(0).email());
    }

    @Test
    void allowsAnEmployeeEditToKeepItsOwnEmailButRejectsAnotherRecordEmail() {
        AppData data = edu.college.employee.service.TestAppData.create();
        var first = data.employees().get(0);
        var second = data.employees().get(1);
        assertThrows(IllegalArgumentException.class, () -> data.changeEmployees(employees -> {
            employees.set(0, new edu.college.employee.model.Employee(first.id(), first.name(), first.department(),
                    first.designation(), second.email(), first.phone(), first.joiningDate(), first.status(), first.salary()));
            return null;
        }));
        assertEquals(first.email(), data.employees().get(0).email());
    }
}

