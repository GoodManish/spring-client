package com.project.springclient.service;

import com.project.springclient.constant.EmployeeConstant;
import com.project.springclient.dto.Employee;
import com.project.springclient.exception.ClientDataException;
import com.project.springclient.exception.EmployeeServiceException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

import static com.project.springclient.constant.EmployeeConstant.*;


@Slf4j
public class EmployeeRestClient {
    private final WebClient webClient;
    public static final Retry fixedRetry = Retry
            .backoff(3, Duration.ofSeconds(2))
//            .filter(e-> e instanceof WebClientResponseException)
            .doAfterRetry((ex) -> {
                log.error("Exception is: {}", ex);
            });

    public EmployeeRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    // http://localhost:8081/employeeservice/v1/allEmployees
    List<Employee> getAllEmployees(){
        return webClient.get().uri(GET_ALL_EMPLOYEES_V1)
                .retrieve()
                .bodyToFlux(Employee.class)
                .collectList()
                .block();

    }

    // http://localhost:8081/employeeservice/v1/employee/2
    Employee getAEmployee(String employeeId){
        try {
            return webClient.get().uri(EMPLOYEE_BY_ID_V1, employeeId)
                    .retrieve()
                    .bodyToMono(Employee.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error responseCode : {}, and Response Body: {}",e.getRawStatusCode(),e.getResponseBodyAsString());
            throw e;
        }
        catch (Exception e) {
            log.error("Error :{}",e.getMessage());
            throw e;
        }
    }

    Employee getAEmployeeWithRetryMechanism(String employeeId) {
        try {
            return webClient.get().uri(EMPLOYEE_BY_ID_V1, employeeId)
                    .retrieve()
                    .bodyToMono(Employee.class)
//                .retry(3)
//                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
//                .retryWhen(fixedRetry)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error responseCode : {}, and Response Body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Error :{}", e.getMessage());
            throw e;
        }

    }

    Employee getAEmployee_with_custom_error_handling(String employeeId){
            return webClient.get().uri(EMPLOYEE_BY_ID_V1, employeeId)
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, clientResponse-> handle4xxError(clientResponse))
                    .onStatus(HttpStatus::is5xxServerError, clientResponse-> handle5xxError(clientResponse))
                    .bodyToMono(Employee.class)
                    .block();

    }

    List<Employee> getEmployeeByName(String name){
        String uriString = UriComponentsBuilder.fromUriString(EMPLOYEE_BY_NAME_V1)
                .queryParam("employee_name", name)
                .build()
                .toUriString();
        return webClient.get().uri(uriString)
                .retrieve()
                .bodyToFlux(Employee.class)
                .collectList()
                .block();
    }

    public Employee addEmployee(Employee employee){
        try {
            return webClient.post().uri(ADD_EMPLOYEE_V1)
    //                .bodyValue(employee)
    //                .body(Mono.just(employee),Employee.class)
                    .body(BodyInserters.fromPublisher(Mono.just(employee), Employee.class))
                    .retrieve()
                    .bodyToMono(Employee.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error responseCode : {}, and Response Body: {}",e.getRawStatusCode(),e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Error in addNewEmployee : {} ",e.getMessage());
            throw e;
        }

    }

    public Employee updateEmployee(int employeeId, Employee employee){
        try {
            return webClient.put().uri(UPDATE_EMPLOYEE_V1, employeeId)
                    .body(BodyInserters.fromPublisher(Mono.just(employee), Employee.class))
                    .retrieve()
                    .bodyToMono(Employee.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error responseCode : {}, and Response Body: {}", e.getRawStatusCode(),e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Error in addNewEmployee : {} ",e.getMessage());
            throw e;
        }
    }

    public String deleteEmployee(int employeeId){
        return webClient.delete().uri(DELETE_EMPLOYEE_V1, employeeId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String errorEndpoint(){
        return webClient.get().uri(ERROR_EMPLOYEE_V1)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse-> handle4xxError(clientResponse))
                .onStatus(HttpStatus::is5xxServerError, clientResponse-> handle5xxError(clientResponse))
                .bodyToMono(String.class)
                .block();
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse clientResponse) {
        Mono<String> errorMsg = clientResponse.bodyToMono(String.class);
        return errorMsg.flatMap(msg -> {
            log.error("Error ResponseCode is: " + clientResponse.rawStatusCode() + " & Response Body :" + msg);
            throw new EmployeeServiceException(msg);
        });
    }

    private Mono<? extends Throwable> handle4xxError(ClientResponse clientResponse) {
        Mono<String> errorMsg = clientResponse.bodyToMono(String.class);
        return errorMsg.flatMap(msg -> {
            log.error("Error ResponseCode is: " + clientResponse.rawStatusCode() + " & Response Body :" + msg);
            throw new ClientDataException(msg);
        });
    }
}
