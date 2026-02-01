package com.restaurantback.services;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.CognitoToken;
import com.restaurantback.dto.EmployeeRequestDTO;
import com.restaurantback.exceptions.AuthorizationException.NotAuthorizedException;
import com.restaurantback.models.Role;
import com.restaurantback.utils.GetAccessTokenFromRequest;
import com.restaurantback.utils.GetDataFromJwt;

import java.util.Map;
import java.util.UUID;

public class EmployeeService {

    private final AmazonDynamoDB amazonDynamoDbClient;
    private final ObjectMapper objectMapper;
    private final String employeeTableName;
    private final String waiterTableName;

    public EmployeeService(AmazonDynamoDB amazonDynamoDbClient, ObjectMapper objectMapper) {
        this.amazonDynamoDbClient = amazonDynamoDbClient;
        this.objectMapper = objectMapper;
        this.employeeTableName = System.getenv("employeeTable");
        this.waiterTableName = System.getenv("waiterTable");
    }

    public void addEmployee(APIGatewayProxyRequestEvent request) {

        String authorityRole = getRoleFromToken(request);
        System.out.println(authorityRole);

        if (authorityRole == null || authorityRole.isEmpty()) {
            throw new RuntimeException("Role of the Authority is not defined");
        }

        if (!authorityRole.toLowerCase().equalsIgnoreCase(Role.ADMIN.getRoleName())) {
            throw new NotAuthorizedException("User is not authorized to perform this task");
        }

        String body = request.getBody();
        System.out.println(body);

        try {
            EmployeeRequestDTO employeeRequestDTO = objectMapper.readValue(body, EmployeeRequestDTO.class);

            if (employeeRequestDTO == null) {
                throw new RuntimeException("Employee details are null");
            }

            GetItemRequest getItemRequest = new GetItemRequest()
                    .withTableName(employeeTableName)
                    .withKey(Map.of("email", new AttributeValue().withS(employeeRequestDTO.getEmail())));

            GetItemResult item = amazonDynamoDbClient.getItem(getItemRequest);

            if (item != null &&
                    item.getItem() != null &&
                    item.getItem().get("email") != null &&
                    item.getItem().get("email").getS().equals(employeeRequestDTO.getEmail())
            ) {
                System.out.println("user with this email already exists");
                throw new RuntimeException("Employee with this email already exists");
            }


            String employeeId = UUID.randomUUID().toString();
            PutItemRequest putItemRequestEmployee = new PutItemRequest()
                    .withTableName(employeeTableName)
                    .withItem(Map.of(
                            "firstName", new AttributeValue().withS(employeeRequestDTO.getFirstName()),
                            "lastName", new AttributeValue().withS(employeeRequestDTO.getLastName()),
                            "role", new AttributeValue().withS(employeeRequestDTO.getRole().toUpperCase()),
                            "email", new AttributeValue().withS(employeeRequestDTO.getEmail()),
                            "locationId", new AttributeValue().withS(employeeRequestDTO.getLocationId()),
                            "employeeId", new AttributeValue().withS(employeeId)
                    ));

            PutItemResult putItemResultEmployee = amazonDynamoDbClient.putItem(putItemRequestEmployee);
            System.out.println(putItemResultEmployee);

            PutItemRequest putItemRequestWaiter = new PutItemRequest()
                    .withTableName(waiterTableName)
                    .withItem(Map.of(
                            "location_id", new AttributeValue().withS(employeeRequestDTO.getLocationId()),
                            "waiter_id", new AttributeValue().withS(employeeId),
                            "isActive", new AttributeValue().withBOOL(true),
                            "shift_start", new AttributeValue().withS("09:00"),
                            "shift_end", new AttributeValue().withS("23:00")
                    ));

            PutItemResult putItemResultWaiter = amazonDynamoDbClient.putItem(putItemRequestWaiter);
            System.out.println(putItemResultWaiter);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    private String getRoleFromToken(APIGatewayProxyRequestEvent request) {

        String idToken = GetAccessTokenFromRequest.getIdToken(request);

        CognitoToken cognitoToken = GetDataFromJwt.extractDataFromToken(idToken);
        System.out.println(cognitoToken.toString() + " " + cognitoToken.getRole());

        return cognitoToken.getRole();
    }
}