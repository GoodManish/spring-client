package com.project.springclient.dto;

import lombok.*;

@ToString(includeFieldNames = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    private Integer id;
    private String firstName;
    private String lastName;
    private Integer age;
    private String gender;
    private String role;

}
