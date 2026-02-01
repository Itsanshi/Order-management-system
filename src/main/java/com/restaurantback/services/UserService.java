package com.restaurantback.services;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.restaurantback.models.User;

import java.util.Map;
import java.util.Optional;

public class UserService {

    private final AmazonDynamoDB amazonDynamoDbClient;
    private final String employeeTableName;
    private final String userTableName;

    public UserService(AmazonDynamoDB amazonDynamoDbClient) {
        this.amazonDynamoDbClient = amazonDynamoDbClient;
        this.employeeTableName = System.getenv("employeeTable");
        this.userTableName = System.getenv("userTable");
    }

    public boolean addUser(User user) {

        try {
            DynamoDB dynamoDB = new DynamoDB(amazonDynamoDbClient);
            Table userTable = dynamoDB.getTable(userTableName);

            Item item = new Item().
                    withPrimaryKey("email", user.getEmail())
                    .withString("cognitoSub", user.getCognitoSub())
                    .withString("firstName", user.getFirstName())
                    .withString("lastName", user.getLastName())
                    .withString("imageUrl", user.getImageUrl())
                    .withString("role", user.getRole().getRoleName().toUpperCase());

            userTable.putItem(item);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<String> userWithEmailAlreadyExists(String email) {
        Map<String, AttributeValue> key = Map.of("email", new AttributeValue().withS(email));

        GetItemRequest searchEmployee = new GetItemRequest()
                .withTableName(employeeTableName)
                .withKey(key);

        GetItemResult res = amazonDynamoDbClient.getItem(searchEmployee);

        if (res.getItem() != null && res.getItem().containsKey("role")) {
            String role = res.getItem().get("role").getS();
            return Optional.of(role);
        }

        return Optional.empty();
    }
}
