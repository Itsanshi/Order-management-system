package com.restaurantback.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class Location {
    private String id; // This is the field that represents the location id
    private String name;
    private String address;
    private String description;
    private String totalCapacity;
    private String averageOccupancy;
    private String image;
    private String rating;
    private String specialityDishes;

    // Partition key for the location, mapped to the attribute "location_id" in DynamoDB
    @DynamoDbPartitionKey
    @DynamoDbAttribute("location_id")
    public String getId() {  // Change method name to getId() to match the field name
        return id;
    }

    // Convert Location to DynamoDB Item
    public Map<String, AttributeValue> toDynamoItem() {
        return Map.of(
                "location_id", AttributeValue.builder().s(id).build(),
                "name", AttributeValue.builder().s(name).build(),
                "address", AttributeValue.builder().s(address).build(),
                "description", AttributeValue.builder().s(description).build(),
                "totalCapacity", AttributeValue.builder().s(totalCapacity).build(),
                "averageOccupancy", AttributeValue.builder().s(totalCapacity).build(),
                "image", AttributeValue.builder().s(image).build(),
                "rating", AttributeValue.builder().s(rating).build(),
                "specialityDishes", AttributeValue.builder().s(specialityDishes).build()
        );
    }
}
