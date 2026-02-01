package com.restaurantback.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class Employee {
    private String email;
    private String id;
    private String firstName;
    private String lastName;
    private String locationId;
    private String role;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("email")
    public String getEmail(){
        return email;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "employee-id-index")
    @DynamoDbAttribute("employeeId")
    public String getId(){
        return id;
    }

}
