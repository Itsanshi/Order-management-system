package com.restaurantback.models;


import com.amazonaws.services.dynamodbv2.xspec.S;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@DynamoDbBean
public class Waiter {

    private String id;
    private String locationId;
    private Map<String, List<String>> booked;
    private boolean isActive;
    private String shiftStart;
    private String shiftEnd;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("waiter_id")
    public String getId(){
        return id;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("location_id")
    public  String getLocationId(){
        return locationId;
    }

    @DynamoDbAttribute("shift_start")
    public String getShiftStart(){
        return shiftStart;
    }

    @DynamoDbAttribute("shift_end")
    public String getShiftEnd(){
        return shiftStart;
    }

}
