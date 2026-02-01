package com.restaurantback.models;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class Table {
    private String id;
    private String locationId;
    private String capacity;

    // Change TimeSlot to a String key (e.g., "10:30-12:00")
    private Map<String, List<String>> booked;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("table_id")
    public String getId() {
        return id;
    }

    @DynamoDbAttribute("location_id")
    public String getLocationId(){
        return locationId;
    }

    // Convert availability map when saving to DynamoDB
    public Map<String, AttributeValue> toDynamoItem() {
        Map<String, AttributeValue> availabilityMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : booked.entrySet()) {
            availabilityMap.put(entry.getKey(), AttributeValue.builder().ss(entry.getValue()).build());
        }

        return Map.of(
                "id", AttributeValue.builder().s(id).build(),
                "locationId", AttributeValue.builder().s(locationId).build(),
                "capacity", AttributeValue.builder().s(capacity).build(),
                "availability", AttributeValue.builder().m(availabilityMap).build()
        );
    }
}

