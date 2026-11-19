package edu.college.employee.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.college.employee.model.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmployeeAlgorithmsTest {
    @Test
    void linearSearchMatchesPartialIdNameAndDepartmentIgnoringCaseWithStatusFilter() {
        Employee alice = employee("EMP001", "Alice Morgan", "Finance", "100.00", "2024-01-01", "ACTIVE");
        Employee bob = employee("EMP002", "Robert Chen", "Operations", "100.00", "2024-01-02", "ON_LEAVE");
        Employee cara = employee("EMP003", "Cara Morgan", "Finance", "100.00", "2024-01-03", "ACTIVE");
        List<Employee> employees = List.of(alice, bob, cara);

        assertEquals(List.of(alice), EmployeeAlgorithms.linearSearch(employees, "p001", ""));
        assertEquals(List.of(alice, cara), EmployeeAlgorithms.linearSearch(employees, "mOr", ""));
        assertEquals(List.of(bob), EmployeeAlgorithms.linearSearch(employees, "OPER", "ON_LEAVE"));
        assertEquals(List.of(alice, cara), EmployeeAlgorithms.linearSearch(employees, "finance", "ACTIVE"));
        assertEquals(List.of(), EmployeeAlgorithms.linearSearch(employees, "missing", ""));
        assertEquals(List.of(), EmployeeAlgorithms.linearSearch(employees, "Morgan", "INACTIVE"));
        assertEquals(List.of(alice, bob, cara), EmployeeAlgorithms.linearSearch(employees, "", ""));
    }

    @Test
    void binarySearchFindsExactIdsInUnsortedInputAndRespectsStatus() {
        Employee first = employee("EMP001", "Ari", "Finance", "100.00", "2024-01-01", "ACTIVE");
        Employee middle = employee("EMP052", "Bea", "Operations", "100.00", "2024-01-02", "ON_LEAVE");
        Employee last = employee("EMP099", "Cam", "Engineering", "100.00", "2024-01-03", "ACTIVE");
        List<Employee> unsorted = List.of(last, middle, first);

        assertEquals(List.of(first), EmployeeAlgorithms.binarySearch(unsorted, "emp001", ""));
        assertEquals(List.of(last), EmployeeAlgorithms.binarySearch(unsorted, "EMP099", "ACTIVE"));
        assertEquals(List.of(middle), EmployeeAlgorithms.binarySearch(unsorted, "emp052", "ON_LEAVE"));
        assertTrue(EmployeeAlgorithms.binarySearch(unsorted, "missing", "").isEmpty());
        assertTrue(EmployeeAlgorithms.binarySearch(unsorted, "EMP052", "ACTIVE").isEmpty());
        assertTrue(EmployeeAlgorithms.binarySearch(List.of(), "EMP001", "").isEmpty());
        assertTrue(EmployeeAlgorithms.binarySearch(unsorted, "", "").isEmpty());
        assertEquals(List.of(last, middle, first), unsorted);
    }

    @Test
    void mergeSortHandlesEmptySingleReverseAndDuplicateIds() {
        assertTrue(EmployeeAlgorithms.mergeSort(List.of(), "id", true).isEmpty());
        assertEquals(List.of("03"), ids(EmployeeAlgorithms.mergeSort(List.of(TestEmployee.of(3)), "id", true)));
        for (var values : List.of(List.of(1, 2, 3), List.of(3, 2, 1), List.of(2, 1, 2, 1))) {
            var employees = values.stream().map(TestEmployee::of).toList();
            assertEquals(values.stream().sorted().map(x -> String.format("%02d", x)).toList(),
                    ids(EmployeeAlgorithms.mergeSort(employees, "id", true)));
        }
    }

    @Test
    void tableMergeSortSupportsFieldsDirectionsAndDoesNotMutateInput() {
        Employee zoe = employee("E2", "Zoe", "Sales", "150.00", "2024-02-01");
        Employee amy = employee("E1", "Amy", "Engineering", "100.00", "2024-01-01");
        Employee bea = employee("E3", "Bea", "Engineering", "200.00", "2024-03-01");
        List<Employee> input = List.of(zoe, amy, bea);

        assertEquals(List.of("E1", "E2", "E3"), ids(EmployeeAlgorithms.mergeSort(input, "id", true)));
        assertEquals(List.of("E3", "E2", "E1"), ids(EmployeeAlgorithms.mergeSort(input, "id", false)));
        assertEquals(List.of("E1", "E3", "E2"), ids(EmployeeAlgorithms.mergeSort(input, "name", true)));
        assertEquals(List.of("E2", "E3", "E1"), ids(EmployeeAlgorithms.mergeSort(input, "name", false)));
        assertEquals(List.of("E1", "E2", "E3"), ids(EmployeeAlgorithms.mergeSort(input, "salary", true)));
        assertEquals(List.of("E3", "E2", "E1"), ids(EmployeeAlgorithms.mergeSort(input, "salary", false)));
        assertEquals(List.of("E1", "E2", "E3"), ids(EmployeeAlgorithms.mergeSort(input, "joiningDate", true)));
        assertEquals(List.of("E3", "E2", "E1"), ids(EmployeeAlgorithms.mergeSort(input, "joiningDate", false)));
        assertEquals(List.of(zoe, amy, bea), input);
        assertThrows(IllegalArgumentException.class, () -> EmployeeAlgorithms.mergeSort(input, "email", true));
        assertThrows(IllegalArgumentException.class, () -> EmployeeAlgorithms.mergeSort(input, "", true));
        assertThrows(IllegalArgumentException.class, () -> EmployeeAlgorithms.mergeSort(input, null, true));
    }

    @Test
    void mergeSortIsStableForEqualKeysInBothDirections() {
        Employee first = employee("emp001", "Same Name", "Finance", "100.00", "2024-01-01");
        Employee second = employee("EMP001", "Same Name", "Finance", "100.00", "2024-01-01");
        Employee third = employee("EMP002", "Same Name", "Finance", "100.00", "2024-01-01");
        List<Employee> input = List.of(first, second, third);

        assertEquals(input, EmployeeAlgorithms.mergeSort(input, "id", true));
        assertEquals(List.of(third, first, second), EmployeeAlgorithms.mergeSort(input, "id", false));
        for (String field : List.of("name", "salary", "joiningDate")) {
            assertEquals(input, EmployeeAlgorithms.mergeSort(input, field, true));
            assertEquals(input, EmployeeAlgorithms.mergeSort(input, field, false));
        }
    }

    @Test
    void employeePageFilteringCanBeSortedWithoutChangingStoredOrder() {
        Employee first = employee("E3", "Alana", "Sales", "90.00", "2024-03-01", "ACTIVE");
        Employee second = employee("E1", "Alana", "Sales", "70.00", "2024-01-01", "ACTIVE");
        Employee excluded = employee("E2", "Alana", "Sales", "80.00", "2024-02-01", "INACTIVE");
        List<Employee> stored = List.of(first, second, excluded);

        List<Employee> filtered = EmployeeAlgorithms.linearSearch(stored, "alana", "ACTIVE");
        assertEquals(List.of("E1", "E3"), ids(EmployeeAlgorithms.mergeSort(filtered, "id", true)));
        assertEquals(List.of(first, second, excluded), stored);
    }

    private static List<String> ids(List<Employee> employees) {
        return employees.stream().map(Employee::id).toList();
    }

    private static Employee employee(String id, String name, String department, String salary, String joiningDate) {
        return employee(id, name, department, salary, joiningDate, "ACTIVE");
    }

    private static Employee employee(String id, String name, String department, String salary, String joiningDate,
                                     String status) {
        return new Employee(id, name, department, "Analyst", id.toLowerCase() + "@example.test", "555-0100",
                LocalDate.parse(joiningDate), status, new BigDecimal(salary));
    }
}
