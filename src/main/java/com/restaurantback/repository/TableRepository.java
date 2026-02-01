package com.restaurantback.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.restaurantback.models.Table;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public class  TableRepository {
    private final AmazonDynamoDB dynamoDB;
    private final String tableName = System.getenv("tablesTable");
    private final DynamoDbEnhancedClient enhancedClient;

    @Inject
    public TableRepository(AmazonDynamoDB dynamoDB, DynamoDbEnhancedClient enhancedClient) {
        this.dynamoDB = dynamoDB;
        this.enhancedClient = enhancedClient;
        System.out.println("[TableRepository] Initialized with DynamoDB client");
    }

    public Map<String, AttributeValue> getTableDataById(String tableId, String locationId) {
        System.out.println("[getTableDataById] Fetching table: " + tableId + " at location: " + locationId);

        GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of(
                        "table_id", new AttributeValue().withS(tableId),
                        "location_id", new AttributeValue().withS(locationId)
                ));

        GetItemResult result = dynamoDB.getItem(getItemRequest);
        if (result.getItem() == null) {
            System.out.println("[getTableDataById] Table not found");
        } else {
            System.out.println("[getTableDataById] Successfully retrieved table data");
        }
        return result.getItem();
    }

    public boolean doesTableExist(String tableId, String locationId) {
        System.out.println("[doesTableExist] Checking existence of table: " + tableId);
        boolean exists = getTableDataById(tableId, locationId) != null;
        System.out.println("[doesTableExist] Table exists: " + exists);
        return exists;
    }


    public Map<String, List<String>> getBookedTimeSlots(String tableId, String locationId) {
        System.out.println("[getBookedTimeSlots] Getting bookings for table: " + tableId);

        Map<String, AttributeValue> tableData = getTableDataById(tableId, locationId);

        if (tableData == null || !tableData.containsKey("booked")) {
            System.out.println("[getBookedTimeSlots] No bookings found");
            return Map.of();
        }

        Map<String, List<String>> slots = tableData.get("booked").getM().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getL().stream()
                                .map(AttributeValue::getS)
                                .collect(Collectors.toList())
                ));

        System.out.println("[getBookedTimeSlots] Found " + slots.size() + " booking dates");
        return slots;
    }

    public void updateTimeSlotForTableWithId(String tableId, String locationId, String date, String newTimeSlot) {
        // Define the key for the item to update
        Map<String, AttributeValue> key = Map.of(
                "table_id", new AttributeValue().withS(tableId),
                "location_id", new AttributeValue().withS(locationId)
        );

        GetItemRequest getRequest = new GetItemRequest()
                .withTableName(tableName)
                .withKey(key);

        Map<String, AttributeValue> currentItem = dynamoDB.getItem(getRequest).getItem();
        if (currentItem == null) {
            throw new RuntimeException("Table not found");
        }

        // Prepare the update
        UpdateItemRequest updateRequest;

        if (!currentItem.containsKey("booked")) {
            // Case 1: Create new booked map
            Map<String, AttributeValue> newMap = new HashMap<>();
            newMap.put(date, new AttributeValue().withL(List.of(new AttributeValue().withS(newTimeSlot))));

            updateRequest = new UpdateItemRequest()
                    .withTableName(tableName)
                    .withKey(key)
                    .withUpdateExpression("SET booked = :newMap")
                    .withExpressionAttributeValues(Map.of(
                            ":newMap", new AttributeValue().withM(newMap)
                    ));
        } else {
            // Case 2: Update existing booked
            updateRequest = new UpdateItemRequest()
                    .withTableName(tableName)
                    .withKey(key)
                    .withUpdateExpression("SET booked.#date = list_append(if_not_exists(booked.#date, :emptyList), :newSlot)")
                    .withExpressionAttributeNames(Map.of("#date", date))
                    .withExpressionAttributeValues(Map.of(
                            ":emptyList", new AttributeValue().withL(new ArrayList<>()),
                            ":newSlot", new AttributeValue().withL(List.of(new AttributeValue().withS(newTimeSlot)))
                    ));
        }
        System.out.println(key);
        dynamoDB.updateItem(updateRequest);

        System.out.println("Time slot updated successfully for tableId: " + tableId + ", locationId: " + locationId + ", date: " + date);
    }

    public void removeTimeSlotFromTable(String tableId, String locationId, String date, String timeSlot) {
        System.out.println("[removeTimeSlot] Removing slot: " + timeSlot + " from table: " + tableId);

        int index = getTimeSlotIndex(tableId, locationId, date, timeSlot);

        if (index != -1) {
            System.out.println("[removeTimeSlot] Found slot at index: " + index);

            UpdateItemRequest updateRequest = new UpdateItemRequest()
                    .withTableName(tableName)
                    .withKey(Map.of(
                            "table_id", new AttributeValue().withS(tableId),
                            "location_id", new AttributeValue().withS(locationId)
                    ))
                    .withUpdateExpression("REMOVE booked.#date[" + index + "]")
                    .withExpressionAttributeNames(Map.of("#date", date));

            dynamoDB.updateItem(updateRequest);
            System.out.println("[removeTimeSlot] Slot removed successfully");
        } else {
            System.out.println("[removeTimeSlot] Slot not found - nothing to remove");
        }
    }

    private int getTimeSlotIndex(String id, String locationId, String date, String timeSlot) {
        System.out.println("[getTimeSlotIndex] Locating slot: " + timeSlot + " for date: " + date);

        GetItemRequest getRequest = new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of(
                        "table_id", new AttributeValue().withS(id),
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

            for (int i = 0; i < timeSlots.size(); i++) {
                if (timeSlots.get(i).getS().equals(timeSlot)) {
                    System.out.println("[getTimeSlotIndex] Found slot at index: " + i);
                    return i;
                }
            }
        }

        System.out.println("[getTimeSlotIndex] Slot not found");
        return -1;
    }

    public boolean doesTableExistsWithIdAndLocationId(String tableId,String locationId){
        GetItemRequest getItemRequest = new GetItemRequest(tableName,Map.of(
                "table_id",new AttributeValue(tableId),
                "location_id",new AttributeValue(locationId))
        ).withProjectionExpression("table_id");
        return dynamoDB.getItem(getItemRequest).getItem()!=null;
    }

    public AttributeValue getNumberOfGuestsForTableWithIdAndLocationId(String tableId,String locationId){
        GetItemRequest getItemRequest = new GetItemRequest(tableName,Map.of("table_id",new AttributeValue(tableId),"location_id",new AttributeValue(locationId)))
                .withProjectionExpression("#capacity")
                .withExpressionAttributeNames(Map.of("#capacity","capacity"));
        Map<String,AttributeValue> result  =  dynamoDB.getItem(getItemRequest).getItem();
        return result==null?null:result.get("capacity");
    }

    public Table getTableByIdAndLocationId(String tableNumber, String locationId) {
        DynamoDbTable<Table> table = enhancedClient.table(tableName, TableSchema.fromBean(Table.class));
        List<Table> tables = new ArrayList<>();
        table.scan().items().forEach(tables::add);

        return tables.stream()
                .filter(table1 -> table1.getId().equalsIgnoreCase(tableNumber) && table1.getLocationId().equalsIgnoreCase(locationId))
                .findFirst()
                .orElse(null);
    }
}