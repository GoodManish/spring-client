package com.project.springclient.service;

import com.project.springclient.dto.Employee;
import com.project.springclient.exception.ClientDataException;
import com.project.springclient.exception.EmployeeServiceException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.equalTo;

class EmployeeRestClientTest {

    private static final String baseUrl = "http://localhost:8081/employeeservice";
    private final WebClient webClient = WebClient.create(baseUrl);
    EmployeeRestClient employeeRestClient = new EmployeeRestClient(webClient);


    @Test
    void getAEmployee() {
        Employee employee = employeeRestClient.getAEmployee("2");
        assertThat(employee).isNotNull();
        assertThat(employee.getFirstName()).isEqualTo("Adam");
    }


    @Test
    void getAllEmployees() {
        List<Employee> allEmployees = employeeRestClient.getAllEmployees();
        System.out.println("allEmployees = " + allEmployees);
        assertThat(allEmployees).isNotEmpty().isNotNull();
    }

    @Test
    void employee_Not_Found_Exception(){
        String employeeId = "999";
        org.junit.jupiter.api.Assertions.assertThrows(WebClientException.class,
                () -> employeeRestClient.getAEmployee(employeeId));
    }

    @Test
    void employee_With_Retry_Mechanism(){
        String employeeId = "999";
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> employeeRestClient.getAEmployeeWithRetryMechanism(employeeId));
    }

    @Test
    void employee_with_custom_error_handling(){
        String employeeId = "999";
        org.junit.jupiter.api.Assertions.assertThrows(ClientDataException.class,
                () -> employeeRestClient.getAEmployee_with_custom_error_handling(employeeId));
    }

    @Test
    void getEmployeeByName() {
        String name = "Chris";
        List<Employee> employeeList = employeeRestClient.getEmployeeByName(name);
        System.out.println("employeeList = " + employeeList);
        assertThat(employeeList).hasSize(1);
        assertThat(employeeList.get(0).getFirstName()).isEqualTo("Chris");
    }

    @Test
    void getEmployeeByName_Not_Found() {
        String name = "Mani";
        org.junit.jupiter.api.Assertions.assertThrows(WebClientResponseException.class, () -> employeeRestClient.getEmployeeByName(name));
    }


    @Test
    void addEmployee() {
        Employee employee =
                new Employee(null, "Iron", "man",
                            34, "male", "developer");
        Employee addedEmployee = employeeRestClient.addEmployee(employee);
        System.out.println("addedEmployee = " + addedEmployee);
        assertThat(addedEmployee).isNotNull()
                                .matches(x-> x.getFirstName().equalsIgnoreCase("iron"));
    }
    @Test
    void addEmployee_Exception() {
        Employee employee =
                new Employee(null, null, null,34, "male", "developer");
        org.junit.jupiter.api.Assertions.assertThrows(WebClientResponseException.class,
                ()->employeeRestClient.addEmployee(employee));
    }

    @Test
    void update_Employee() {
        Employee employee =
                new Employee(null, "Adam1", "Sandler1",
                        50, "male", "developer");
        Employee addedEmployee = employeeRestClient.updateEmployee(2, employee);
        System.out.println("updatedEmployee = " + addedEmployee);
        assertThat(addedEmployee)
                .isNotNull()
                .matches(x-> x.getFirstName().equalsIgnoreCase("adam1"))
                .matches(x-> x.getId().equals(2));
    }

    @Test
    void update_Employee_exception() {
        Employee employee =
                new Employee(null, null, null,
                        50, "male", "developer");
        org.junit.jupiter.api.Assertions.assertThrows(WebClientResponseException.class,
                ()->employeeRestClient.updateEmployee(444, employee));
    }

    @Test
    void deleteEmployee() {
        Employee employee =
                new Employee(null, "Iron1", "man1",
                        22, "male", "developer1");
        Employee addedEmployee = employeeRestClient.addEmployee(employee);
        System.out.println("addedEmployee = " + addedEmployee);
        assertThat(addedEmployee).isNotNull();
        String deleteEmployee = employeeRestClient.deleteEmployee(addedEmployee.getId());
        assertThat(deleteEmployee).isEqualTo("Employee deleted successfully.");

    }

    @Test
    void error_5xx_status_Endpoint() {
        org.junit.jupiter.api.Assertions.assertThrows(EmployeeServiceException.class,
                ()-> employeeRestClient.errorEndpoint());
    }

}