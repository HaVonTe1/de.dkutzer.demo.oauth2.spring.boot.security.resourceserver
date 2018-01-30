package de.dbfuhrpark.demo.spring.boot.security.oauth2.server.resourceserver.person.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Person {


    private String firstname;
    private String lastname;

    private int age;


}
