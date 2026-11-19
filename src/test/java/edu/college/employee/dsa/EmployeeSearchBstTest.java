package edu.college.employee.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.college.employee.model.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmployeeSearchBstTest {
    @Test
    void insertsByEmployeeIdSearchesBothBranchesAndRejectsDuplicateIds() {
        EmployeeSearchBst bst = new EmployeeSearchBst();
        Employee root = employee("EMP052");
        Employee left = employee("EMP020");
        Employee right = employee("EMP080");
        Employee leftChild = employee("EMP010");
        Employee rightChild = employee("EMP090");

        assertNull(bst.search("EMP052"));
        for (Employee record : List.of(root, left, right, leftChild, rightChild)) assertTrue(bst.insert(record));
        assertFalse(bst.insert(employee("emp052")));
        assertEquals(5, bst.size());
        assertEquals(root, bst.search("emp052"));
        assertEquals(left, bst.search("EMP020"));
        assertEquals(leftChild, bst.search("EMP010"));
        assertEquals(right, bst.search("EMP080"));
        assertEquals(rightChild, bst.search("EMP090"));
        assertNull(bst.search("EMP999"));
        assertNull(bst.search(""));
    }

    @Test
    void rebuildReplacesUpdatedAndDeletedEmployeeRecords() {
        EmployeeSearchBst bst = new EmployeeSearchBst();
        Employee original = employee("EMP001");
        bst.rebuild(List.of(original, employee("EMP002")));
        Employee updated = new Employee(original.id(), "Updated Person", original.department(), original.designation(),
                original.email(), original.phone(), original.joiningDate(), original.status(), original.salary());

        bst.rebuild(List.of(updated));

        assertEquals(1, bst.size());
        assertEquals(updated, bst.search("EMP001"));
        assertNull(bst.search("EMP002"));
        bst.rebuild(List.of());
        assertEquals(0, bst.size());
        assertNull(bst.search("EMP001"));
    }

    private static Employee employee(String id) {
        return new Employee(id, "Employee " + id, "Finance", "Analyst", id.toLowerCase() + "@example.test",
                "555-0100", LocalDate.parse("2024-01-01"), "ACTIVE", new BigDecimal("50000.00"));
    }
}
