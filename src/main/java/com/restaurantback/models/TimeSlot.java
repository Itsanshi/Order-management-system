package com.restaurantback.models;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class TimeSlot {
    private String id;
    private String from;
    private String to;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("timeslot_id")  // Ensures correct mapping in DynamoDB
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbAttribute("from")  // Matches lowercase "from" in DynamoDB
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    @DynamoDbAttribute("to")  // Matches lowercase "to" in DynamoDB
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    // Convert to DynamoDB item
    public Map<String, AttributeValue> toDynamoItem() {
        return Map.of(
                "id", AttributeValue.builder().s(id).build(),
                "from", AttributeValue.builder().s(from).build(),
                "to", AttributeValue.builder().s(to).build()
        );
    }
}
