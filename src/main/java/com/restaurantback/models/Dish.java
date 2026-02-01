package com.restaurantback.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class Dish {
    private String id;
    private String name;
    private String price;
    private String  weight;
    public String image;
    private String state;
    private String calories;
    private String carbs;
    private String description;
    private String dishType;
    private String fats;
    private String proteins;
    private String vitamins;
    private boolean isSpecial;
    private String feedbackId;
    private boolean isAvailable;
    private boolean isPopular;
    private String popularityScore;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("dish_id")
    public String getId() {
        return id;
    }

    @DynamoDbAttribute("isSpecial")
    public boolean isSpecial(){
        return isSpecial;
    }

    @DynamoDbAttribute("isAvailable")
    public boolean isAvailable(){
        return isAvailable;
    }

    @DynamoDbAttribute("isPopular")
    public boolean isPopular(){
        return isPopular;
    }

    public Map<String, AttributeValue> toDynamoItem() {
        Map<String, AttributeValue> dynamoItem = new HashMap<>();

        dynamoItem.put("dish_id", AttributeValue.builder().s(id).build());
        dynamoItem.put("name", AttributeValue.builder().s(name).build());
        dynamoItem.put("price", AttributeValue.builder().s(price).build());
        dynamoItem.put("weight", AttributeValue.builder().s(weight).build());
        dynamoItem.put("image", AttributeValue.builder().s(image).build());
        dynamoItem.put("state", AttributeValue.builder().s(state).build());
        dynamoItem.put("calories", AttributeValue.builder().s(calories).build());
        dynamoItem.put("carbs", AttributeValue.builder().s(carbs).build());
        dynamoItem.put("description", AttributeValue.builder().s(description).build());
        dynamoItem.put("dish_type", AttributeValue.builder().s(dishType).build());
        dynamoItem.put("fats", AttributeValue.builder().s(fats).build());
        dynamoItem.put("proteins", AttributeValue.builder().s(proteins).build());
        dynamoItem.put("vitamins", AttributeValue.builder().s(vitamins).build());
        dynamoItem.put("is_special", AttributeValue.builder().bool(isSpecial).build());
        dynamoItem.put("feedback_id", AttributeValue.builder().s(feedbackId).build());
        dynamoItem.put("is_available", AttributeValue.builder().bool(isAvailable).build());
        dynamoItem.put("is_popular", AttributeValue.builder().bool(isPopular).build());

        return dynamoItem;
    }

}
