package com.restaurantback.models;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.UUID;

@DynamoDbBean
@Data
public class Feedback {

    private String id;
    private String rate;
    private String comment;
    private String userName;
    private String date;
    private String type;
    private String locationID;
    private String cuisineComment;
    private String cuisineRating;
    private String resID;
    private String serviceComment;
    private String serviceRating;
    private String userAvatarUrl;

    public Feedback() {
        this.id = UUID.randomUUID().toString();
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("feedback_id")
    public String getId() {
        return id;
    }

}

