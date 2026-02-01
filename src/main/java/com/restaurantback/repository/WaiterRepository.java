package com.restaurantback.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.*;
import com.restaurantback.models.Booking;
import com.restaurantback.models.Location;
import com.restaurantback.models.Waiter;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import javax.inject.Inject;
import java.util.*;

public class WaiterRepository {
    private final AmazonDynamoDB dynamoDB;
    private final String waiters = System.getenv("waiterTable");
    private final DynamoDbEnhancedClient enhancedClient;

    @Inject
    public WaiterRepository(AmazonDynamoDB dynamoDB, DynamoDbEnhancedClient enhancedClient) {
        this.dynamoDB = dynamoDB;
        this.enhancedClient = enhancedClient;
    }

    public List<Map<String, AttributeValue>> getWaitersByLocation(String locationId) {
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(waiters)
                .withFilterExpression("location_id = :locationId AND isActive = :isActive")
                .withExpressionAttributeValues(Map.of(
                        ":locationId", new AttributeValue().withS(locationId),
                        ":isActive", new AttributeValue().withBOOL(true)
                ));

        ScanResult scanResult = dynamoDB.scan(scanRequest);
        return scanResult.getItems() != null ? scanResult.getItems() : Collections.emptyList();
    }

    public Map<String, AttributeValue> getWaiterByIdAndLocationId(String waiterId, String locationId) {
        GetItemRequest getRequest = new GetItemRequest()
                .withTableName(waiters)
                .withKey(Map.of(
                        "waiter_id", new AttributeValue().withS(waiterId),
                        "location_id", new AttributeValue().withS(locationId)
                ));

        return dynamoDB.getItem(getRequest).getItem();
    }

    public void updateTimeSlotForWaiterWithId(String waiterId, String locationId, String date, String newTimeSlot) {
        try {
            // Log the input parameters
            System.out.println("Starting updateTimeSlotForWaiterWithId...");
            System.out.println("Input Parameters:");
            System.out.println("Waiter ID: " + waiterId);
            System.out.println("Location ID: " + locationId);
            System.out.println("Date: " + date);
            System.out.println("New Time Slot: " + newTimeSlot);

            // Validate input parameters
            if (waiterId == null || waiterId.isEmpty()) {
                throw new IllegalArgumentException("waiterId cannot be null or empty");
            }
            if (locationId == null || locationId.isEmpty()) {
                throw new IllegalArgumentException("locationId cannot be null or empty");
            }
            if (date == null || date.isEmpty()) {
                throw new IllegalArgumentException("date cannot be null or empty");
            }
            if (newTimeSlot == null || newTimeSlot.isEmpty()) {
                throw new IllegalArgumentException("newTimeSlot cannot be null or empty");
            }

            DescribeTableRequest describeRequest = new DescribeTableRequest()
                    .withTableName(waiters);
            TableDescription tableDesc = dynamoDB.describeTable(describeRequest).getTable();
            System.out.println("Table Key Schema: " + tableDesc.getKeySchema());

            System.out.println("Key DEBUG: " +
                    "waiter_id=" + waiterId.getBytes().length + " bytes, " +
                    "location_id=" + locationId.getBytes().length + " bytes");

            GetItemRequest getRequest = new GetItemRequest()
                    .withTableName(waiters)
                    .withKey(Map.of(
                            "waiter_id", new AttributeValue().withS(waiterId),
                            "location_id", new AttributeValue().withS(locationId)
                    ));

            System.out.println(getRequest.toString());

            Map<String, AttributeValue> currentItem = dynamoDB.getItem(getRequest).getItem();

            System.out.println(currentItem.toString());
            // If the item doesn't exist, throw an exception
            if (currentItem == null) {
                throw new RuntimeException("Waiter not found with id: " + waiterId + " and location: " + locationId);
            }

            // Prepare the update
            String updateExpression;
            Map<String, AttributeValue> attributeValues = new HashMap<>();
            Map<String, String> attributeNames = new HashMap<>(); // Start empty

            if (!currentItem.containsKey("booked")) {
                // Create the entire structure (no #date used)
                updateExpression = "SET booked = :newMap";
                Map<String, AttributeValue> newMap = new HashMap<>();
                newMap.put(date, new AttributeValue().withL(List.of(new AttributeValue().withS(newTimeSlot))));
                attributeValues.put(":newMap", new AttributeValue().withM(newMap));
            } else {
                // Update existing structure (#date is used)
                updateExpression = "SET booked.#date = list_append(if_not_exists(booked.#date, :emptyList), :newSlot)";
                attributeNames.put("#date", date); // Add #date ONLY here
                attributeValues.put(":emptyList", new AttributeValue().withL(new ArrayList<>()));
                attributeValues.put(":newSlot", new AttributeValue().withL(List.of(new AttributeValue().withS(newTimeSlot))));
            }

            UpdateItemRequest updateRequest = new UpdateItemRequest()
                    .withTableName(waiters)
                    .withKey(Map.of(
                            "waiter_id", new AttributeValue().withS(waiterId),
                            "location_id", new AttributeValue().withS(locationId)
                    ))
                    .withUpdateExpression(updateExpression)
                    .withExpressionAttributeValues(attributeValues);

// Only add attributeNames if not empty
            if (!attributeNames.isEmpty()) {
                updateRequest.withExpressionAttributeNames(attributeNames);
            }


            System.out.println("Update Expression: " + updateExpression);
            System.out.println("Attribute Names: " + attributeNames);
//
//            // Log the update request
            System.out.println("Update Request: " + updateRequest);
//
//            // Execute the update
            dynamoDB.updateItem(updateRequest);

            // Log success
            System.out.println("Time slot updated successfully for waiterId: " + waiterId + ", locationId: " + locationId + ", date: " + date);
        }
        catch (Exception e) {
            // Log the exception
            System.err.println("Error occurred while updating time slot:");
            e.printStackTrace();
            throw e; // Re-throw the exception to propagate it further
        }
    }

    // In WaiterRepository
    public void removeTimeSlotFromWaiter(String waiterId, String locationId, String date, String timeSlot) {
        // First get the index of the time slot (one-time read)
        int index = getTimeSlotIndex(waiterId, locationId, date, timeSlot);

        if (index != -1) {  // Only proceed if time slot exists
            UpdateItemRequest updateRequest = new UpdateItemRequest()
                    .withTableName(waiters)  // Using waiters table name
                    .withKey(Map.of(
                            "waiter_id", new AttributeValue().withS(waiterId),  // Using waiter_id as key
                            "location_id", new AttributeValue().withS(locationId)
                    ))
                    .withUpdateExpression("REMOVE booked.#date[" + index + "]")
                    .withExpressionAttributeNames(Map.of("#date", date));

            dynamoDB.updateItem(updateRequest);
        }
    }

    public int getTimeSlotIndex(String waiterId, String locationId, String date, String timeSlot) {
        GetItemRequest getRequest = new GetItemRequest()
                .withTableName(waiters)  // Using waiters table name
                .withKey(Map.of(
                        "waiter_id", new AttributeValue().withS(waiterId),  // Using waiter_id as key
                        "location_id", new AttributeValue().withS(locationId)
                ))
                .withProjectionExpression("booked.#date")
                .withExpressionAttributeNames(Map.of("#date", date));

        Map<String, AttributeValue> item = dynamoDB.getItem(getRequest).getItem();

        if (item != null && item.containsKey("booked")) {
            List<AttributeValue> timeSlots = item.get("booked")
                    .getM()
                    .get(date)
                    .getL();

            // Find the index of the specific time slot
            for (int i = 0; i < timeSlots.size(); i++) {
                if (timeSlots.get(i).getS().equals(timeSlot)) {
                    return i;
                }
            }
        }
        return -1;  // Return -1 if not found
    }

    public boolean doesWaiterExistWithIdAndLocationId(String waiterId,String locationId){
        GetItemRequest getItemRequest = new GetItemRequest(waiters,Map.of(
                "waiter_id",new AttributeValue(waiterId),
                "location_id",new AttributeValue(locationId))
        ).withProjectionExpression("waiter_id");
        return dynamoDB.getItem(getItemRequest).getItem()!=null;
    }

    public Waiter getWaiterById(String waiterId) {
        DynamoDbTable<Waiter> table = enhancedClient.table(waiters, TableSchema.fromBean(Waiter.class));
        List<Waiter> waiterList = new ArrayList<>();
        table.scan().items().forEach(waiterList::add);

        return waiterList.stream()
                .filter(waiter -> waiter.getId().equalsIgnoreCase(waiterId))
                .findFirst()
                .orElse(null);
    }
}