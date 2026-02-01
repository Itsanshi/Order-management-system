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
public class LocationReport {

    private String date;
    private String locationId;
    private int orderProcessed;
    private float averageCuisineFeedback;
    private float minimumCuisineFeedback;
    private float revenue;
    private int feedbackCount;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("local_date")
    public String getDate() {
        return date;
    }

    @DynamoDbAttribute("local_date")
    public void setDate(String date) {
        this.date = date;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("location_id")
    public String getLocationId() {
        return locationId;
    }

    @DynamoDbAttribute("location_id")
    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    @DynamoDbAttribute("order_processed")
    public int getOrderProcessed() {
        return orderProcessed;
    }

    @DynamoDbAttribute("order_processed")
    public void setOrderProcessed(int orderProcessed) {
        this.orderProcessed = orderProcessed;
    }

    @DynamoDbAttribute("average_cuisine_feedback")
    public float getAverageCuisineFeedback() {
        return averageCuisineFeedback;
    }

    @DynamoDbAttribute("average_cuisine_feedback")
    public void setAverageCuisineFeedback(float averageCuisineFeedback) {
        this.averageCuisineFeedback = averageCuisineFeedback;
    }

    @DynamoDbAttribute("minimum_cuisine_feedback")
    public float getMinimumCuisineFeedback() {
        return minimumCuisineFeedback;
    }

    @DynamoDbAttribute("minimum_cuisine_feedback")
    public void setMinimumCuisineFeedback(float minimumCuisineFeedback) {
        this.minimumCuisineFeedback = minimumCuisineFeedback;
    }

    @DynamoDbAttribute("revenue")
    public float getRevenue() {
        return revenue;
    }

    @DynamoDbAttribute("revenue")
    public void setRevenue(float revenue) {
        this.revenue = revenue;
    }

    @DynamoDbAttribute("feedback_count")
    public int getFeedbackCount() {
        return feedbackCount;
    }

    @DynamoDbAttribute("feedback_count")
    public void setFeedbackCount(int feedbackCount) {
        this.feedbackCount = feedbackCount;
    }
}
