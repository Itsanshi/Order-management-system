package com.restaurantback.services;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.restaurantback.dto.CognitoToken;
import com.restaurantback.dto.profile.ProfileResponse;
import com.restaurantback.dto.profile.PutUsersProfileRequest;
import com.restaurantback.utils.Base64ImageHandler;
import com.restaurantback.utils.GetAccessTokenFromRequest;
import com.restaurantback.utils.GetDataFromJwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileService {

    private final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    private final AmazonDynamoDB amazonDynamoDbClient;
    private final String userTableName;
    private final Base64ImageHandler base64ImageHandler;

    public ProfileService(AmazonDynamoDB amazonDynamoDbClient, Base64ImageHandler base64ImageHandler) {
        this.amazonDynamoDbClient = amazonDynamoDbClient;
        this.base64ImageHandler = base64ImageHandler;
        this.userTableName = System.getenv("userTable");
    }

    public ProfileResponse findUserByEmail(APIGatewayProxyRequestEvent request) {

        String email = getEmailFromToken(request);
        QueryResult result = getUser(email);

        return result.getItems()
                .stream()
                .map(item -> {
                    return new ProfileResponse(
                            item.get("firstName").getS(),
                            item.get("imageUrl").getS(),
                            item.get("lastName").getS()
                    );
                }).findFirst()
                .orElseThrow(() -> new RuntimeException("user not found"));
    }


    public void updateUserProfile(APIGatewayProxyRequestEvent request, PutUsersProfileRequest putUsersProfileRequest) {
        logger.info("update user profile method...");

        String email = getEmailFromToken(request);
        QueryResult result = getUser(email);
        logger.info("query result: {}", result);

        if (result == null || result.getItems() == null) {
            throw new RuntimeException("User not found");
        }

        String previousImageUrl = result.getItems()
                .stream()
                .map(item -> item.get("imageUrl").getS())
                .findFirst()
                .orElse("");

        String imageUrl;

        if (putUsersProfileRequest.getBase64encodedImage().isEmpty()) {
            imageUrl = previousImageUrl;
        } else {
            imageUrl = base64ImageHandler.handleBase64Image(putUsersProfileRequest.getBase64encodedImage());
        }

        logger.info("presigned image url: {}", imageUrl);

        logger.info("Updating data in user table...");
        Map<String, AttributeValue> key = Map.of(
                "email", new AttributeValue().withS(email) // Replace "email" with your actual primary key attribute name
        );

        // Define the update expression
        String updateExpression = "SET firstName = :firstName, lastName = :lastName, imageUrl = :imageUrl";

        // Define the expression attribute values
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":firstName", new AttributeValue().withS(putUsersProfileRequest.getFirstName()));
        expressionAttributeValues.put(":lastName", new AttributeValue().withS(putUsersProfileRequest.getLastName()));
        expressionAttributeValues.put(":imageUrl", new AttributeValue().withS(imageUrl));

        // Create the UpdateItemRequest
        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(userTableName)
                .withKey(key)
                .withUpdateExpression(updateExpression)
                .withExpressionAttributeValues(expressionAttributeValues);

        UpdateItemResult updateItemResult = amazonDynamoDbClient.updateItem(updateItemRequest);
        logger.info("saving result: {}", updateItemResult);
    }

    private QueryResult getUser(String email) {
        logger.info("query request from dynamoDB for user: {}", email);
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(userTableName)
                .withKeyConditionExpression("email = :email")
                .withExpressionAttributeValues(Map.of(":email", new AttributeValue().withS(email)));

        return amazonDynamoDbClient.query(queryRequest);
    }

    private String getEmailFromToken(APIGatewayProxyRequestEvent request) {
        String idToken = GetAccessTokenFromRequest.getIdToken(request);
        logger.info("id token: {}", idToken);

        CognitoToken cognitoToken = GetDataFromJwt.extractDataFromToken(idToken);
        logger.info("Cognito Token: {}", cognitoToken.toString());

        String email = cognitoToken.getEmail();
        logger.info("Email of user: {}", email);

        return email;
    }
}
