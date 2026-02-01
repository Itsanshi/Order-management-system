package com.restaurantback.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WaiterReport {

    private String date;
    private String email;
    private String name;
    private String locationId;
    private String waiterId;
    private float workingHours = 0.0f;
    private int orderProcessed = 0;
    private float averageServiceFeedback = 0.0f;
    private float minimumServiceFeedback = 0.0f;
    private int feedbackCount = 0;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("local_date")
    public String getDate() {
        return date;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("waiter_id")
    public String getWaiterId() {
        return waiterId;
    }

    @DynamoDbAttribute("waiter_email")
    public String getEmail() {
        return email;
    }

    @DynamoDbAttribute("waiter_name")
    public String getName() {
        return name;
    }

    @DynamoDbAttribute("location_id")
    public String getLocationId() {
        return locationId;
    }

    @DynamoDbAttribute("working_hours")
    public float getWorkingHours() {
        return workingHours;
    }

    @DynamoDbAttribute("orders_processed")
    public int getOrderProcessed() {
        return orderProcessed;
    }

    @DynamoDbAttribute("average_service_feedback")
    public float getAverageServiceFeedback() {
        return averageServiceFeedback;
    }

    @DynamoDbAttribute("minimum_service_feedback")
    public float getMinimumServiceFeedback() {
        return minimumServiceFeedback;
    }

    @DynamoDbAttribute("feedback_count")
    public int getFeedbackCount() {
        return feedbackCount;
    }



    @DynamoDbAttribute("local_date")
    public void setDate(String date) {
        this.date = date;
    }

    @DynamoDbAttribute("waiter_email")
    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDbAttribute("waiter_name")
    public void setName(String name) {
        this.name = name;
    }

    @DynamoDbAttribute("location_id")
    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    @DynamoDbAttribute("waiter_id")
    public void setWaiterId(String waiterId) {
        this.waiterId = waiterId;
    }

    @DynamoDbAttribute("working_hours")
    public void setWorkingHours(float workingHours) {
        this.workingHours = workingHours;
    }

    @DynamoDbAttribute("orders_processed")
    public void setOrderProcessed(int orderProcessed) {
        this.orderProcessed = orderProcessed;
    }

    @DynamoDbAttribute("average_service_feedback")
    public void setAverageServiceFeedback(float averageServiceFeedback) {
        this.averageServiceFeedback = averageServiceFeedback;
    }

    @DynamoDbAttribute("minimum_service_feedback")
    public void setMinimumServiceFeedback(float minimumServiceFeedback) {
        this.minimumServiceFeedback = minimumServiceFeedback;
    }

    @DynamoDbAttribute("feedback_count")
    public void setFeedbackCount(int feedbackCount) {
        this.feedbackCount = feedbackCount;
    }
}
