package com.restaurantback.repository;

import com.restaurantback.models.User;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final String tableName = System.getenv("userTable");

    public UserRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    public String getName(String userEmail) {
        DynamoDbTable<User> table = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(User.class));
        List<User> users = new ArrayList<>();
        table.scan().items().forEach(users::add);

        User user = users.stream()
                .filter(user1 -> user1.getEmail().equalsIgnoreCase(userEmail))
                .findFirst()
                .orElse(null);

        assert user != null;
        return user.getFirstName() + " " + user.getLastName();
    }
}
