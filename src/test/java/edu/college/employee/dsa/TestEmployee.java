package edu.college.employee.dsa;
import edu.college.employee.model.Employee;import java.time.LocalDate;
final class TestEmployee{private TestEmployee(){}static Employee of(int id){String x=String.format("%02d",id);return new Employee(x,"Person "+x,"Engineering","Developer",x+"@example.test","555-0100",LocalDate.of(2024,1,1),"ACTIVE");}}
