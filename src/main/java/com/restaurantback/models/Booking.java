package com.restaurantback.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@AllArgsConstructor
@NoArgsConstructor
@Data
@DynamoDbBean
public class Booking {
    private String reservationId;
    private String locationId;      // Add this
    private String tableId;
    private String date;
    private String waiterId;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;
    private String userEmail;
    private boolean byCustomer;
    private String status;
    private String feedbackId;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("booking_id")
    public String getReservationId(){
        return reservationId;
    }

    @DynamoDbAttribute("locationId")
    public String getLocationId(){ return locationId; }

    @DynamoDbAttribute("table_id")
    public String getTableId() {
        return tableId;
    }

    @DynamoDbAttribute("waiter_id")
    public String getWaiterId() {
        return waiterId;
    }

    @DynamoDbAttribute("from")
    public String getTimeFrom() {
        return timeFrom;
    }

    @DynamoDbAttribute("to")
    public String getTimeTo() {
        return timeTo;
    }

    @DynamoDbAttribute("guests")
    public String getGuestsNumber() {
        return guestsNumber;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("user_email")
    public String getUserEmail() {
        return userEmail;
    }

    @DynamoDbAttribute("user_email")
    public void setUserEmail(String userEmail){
        this.userEmail = userEmail;
    }
}
