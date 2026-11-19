package edu.college.employee.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.college.employee.dsa.EmployeeAlgorithms;
import edu.college.employee.dsa.EmployeeSearchBst;
import edu.college.employee.model.Employee;
import edu.college.employee.service.AppData;
import edu.college.employee.service.TestAppData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmployeeSearchApiTest {
    @Test
    void selectedAlgorithmRunsAgainstTheSameRecordsAndStatusFilter() {
        Employee alice = employee("EMP001", "Alice Morgan", "ACTIVE");
        Employee bob = employee("EMP052", "Bob Chen", "ON_LEAVE");
        Employee cara = employee("EMP099", "Cara Morgan", "ACTIVE");
        List<Employee> unsorted = List.of(cara, bob, alice);
        EmployeeSearchBst bst = new EmployeeSearchBst();
        bst.rebuild(unsorted);

        assertEquals(List.of(cara, alice), ApiController.searchEmployees(unsorted, "mOr", "ACTIVE", "linear", bst::search));
        assertEquals(List.of(bob), ApiController.searchEmployees(unsorted, "emp052", "ON_LEAVE", "binary", bst::search));
        assertEquals(List.of(), ApiController.searchEmployees(unsorted, "EMP052", "ACTIVE", "binary", bst::search));
        assertEquals(List.of(cara), ApiController.searchEmployees(unsorted, "emp099", "ACTIVE", "bst", bst::search));
        assertEquals(List.of(), ApiController.searchEmployees(unsorted, "EMP099", "ON_LEAVE", "bst", bst::search));
        assertThrows(IllegalArgumentException.class,
                () -> ApiController.searchEmployees(unsorted, "EMP001", "", "unknown", bst::search));
    }

    @Test
    void blankQueriesReturnAllRecordsForEveryAlgorithmAndStillRespectStatus() {
        AppData data = TestAppData.create();
        List<Employee> allEmployees = data.employees();
        assertEquals(52, allEmployees.size());
        assertTrue(allEmployees.stream().anyMatch(employee -> employee.id().equalsIgnoreCase("EMP052")));

        for (String algorithm : List.of("linear", "binary", "bst")) {
            assertEquals(allEmployees, ApiController.searchEmployees(allEmployees, "", "", algorithm,
                    data::searchEmployeeInBst));
            assertEquals(allEmployees, ApiController.searchEmployees(allEmployees, "  \t  ", "", algorithm,
                    data::searchEmployeeInBst));

            List<Employee> activeEmployees = allEmployees.stream()
                    .filter(employee -> employee.status().equals("ACTIVE")).toList();
            assertEquals(activeEmployees, ApiController.searchEmployees(allEmployees, " ", "ACTIVE", algorithm,
                    data::searchEmployeeInBst));
        }
    }

    private static Employee employee(String id, String name, String status) {
        return new Employee(id, name, "Finance", "Analyst", id.toLowerCase() + "@example.test", "555-0100",
                LocalDate.parse("2024-01-01"), status, new BigDecimal("50000.00"));
    }
}
