package com.restaurantback.utils;

import com.restaurantback.models.TimeSlot;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.ArrayList;
import java.util.List;

public class TimeslotDB {

    public static TimeSlot getTimeSlotFromDB(String startTime) {

        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        // Access TimeSlot table
        DynamoDbTable<TimeSlot> timeSlotTable = enhancedClient.table(
                System.getenv("timeslotTable"),
                TableSchema.fromBean(TimeSlot.class) // Correctly map TimeSlot
        );

        // Scan the table and collect results
        List<TimeSlot> timeSlots = new ArrayList<>();
        timeSlotTable.scan().items().forEach(timeSlots::add);

        System.out.println(timeSlots);

        // Filter results based on startTime and endTime
        TimeSlot timeSlot =  timeSlots.stream()
                .filter(slot -> slot != null && slot.getFrom() != null && slot.getTo() != null)  // Null check
                .filter(slot -> slot.getFrom().equals(startTime))
                .findFirst()
                .orElse(null);

        System.out.println(timeSlot);
        return timeSlot;
    }
}
