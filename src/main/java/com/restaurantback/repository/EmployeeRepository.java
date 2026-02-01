package com.restaurantback.repository;

import com.restaurantback.models.Employee;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EmployeeRepository {


    private final DynamoDbEnhancedClient enhancedClient;
    private final String employeeTable;
    private final DynamoDbTable<Employee> table;

    public EmployeeRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
        this.employeeTable = System.getenv("employeeTable");
        table = enhancedClient.table(employeeTable, TableSchema.fromBean(Employee.class));

    }

    public String getWaiterIdFromEmail(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        Employee employee = table.getItem(r -> r.key(key));
        return employee.getId();
    }

    public String getName(String email) {
        Key key = Key.builder()
                .partitionValue(email)
                .build();

        Employee employee =  table.getItem(r -> r.key(key));
        System.out.println(employee);
        return employee.getFirstName() + " " + employee.getLastName();
    }

    public String getNameFromId(String waiterId) {
        DynamoDbTable<Employee> table = enhancedClient.table(employeeTable, TableSchema.fromBean(Employee.class));
        List<Employee> employees = new ArrayList<>();
        table.scan().items().forEach(employees::add);

        employees.forEach(System.out::println);
        Employee employee = employees.stream()
                .filter(employee1 -> employee1.getRole().equalsIgnoreCase("waiter") && employee1.getId().equalsIgnoreCase(waiterId))
                .findFirst()
                .orElse(null);

        assert employee != null;
        return employee.getFirstName() + " " + employee.getLastName();
    }

    public Employee getEmployeeWithId(String employeeId) {
        System.out.println("Searching for employee with ID: " + employeeId);

        // Access the GSI
        DynamoDbIndex<Employee> employeeIdIndex = table.index("employee-id-index");

        // Create the key condition for the query
        Key key = Key.builder()
                .partitionValue(employeeId)
                .build();

        QueryConditional queryConditional = QueryConditional.keyEqualTo(key);

        System.out.println("searching for employee");
        // Query the GSI and get the first matching item
        Employee e = employeeIdIndex.query(r -> r.queryConditional(queryConditional))
                .stream()
                .flatMap(page -> page.items().stream())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Employee not found for ID: " + employeeId));

        System.out.println(e.toString());
        return  e;
    }

    public String getLocationIdFromEmail(String email) {
        DynamoDbTable<Employee> table = enhancedClient.table(employeeTable, TableSchema.fromBean(Employee.class));
        List<Employee> employees = new ArrayList<>();
        table.scan().items().forEach(employees::add);

        return Objects.requireNonNull(employees.stream()
                .filter(employee -> employee.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null)).getLocationId();
    }
}
