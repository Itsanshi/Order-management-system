package com.restaurantback.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.xspec.B;
import com.restaurantback.dto.WaiterBookingDTO;

import javax.inject.Inject;

import com.restaurantback.exceptions.reservationException.NotFoundException;
import com.restaurantback.models.Booking;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.awt.print.Book;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BookingRepository {
    private final AmazonDynamoDB dynamoDB;
    private final DynamoDbEnhancedClient enhancedClient;
    private final String bookingTable = System.getenv("bookingTable"); // Ensure the table name matches DynamoDB

    @Inject
    public BookingRepository(AmazonDynamoDB dynamoDB, DynamoDbEnhancedClient enhancedClient) {
        this.dynamoDB = dynamoDB;
        this.enhancedClient = enhancedClient;
    }

    public List<Map<String, AttributeValue>> getBookingsByEmail(String userEmail) {
        System.out.println("In Repository Layer getReservations line 25 before the actual query");
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(bookingTable)
                .withFilterExpression("user_email = :email")
                .withExpressionAttributeValues(Map.of(
                        ":email", new AttributeValue().withS(userEmail)
                ));
        System.out.println("Before Scan in Repository");
        ScanResult scanResult = dynamoDB.scan(scanRequest);
        System.out.println("After Scan in Repository");
        return scanResult.getItems();
    }


    public boolean doesBookingExistForUserWithEmailAndId(String reservationId,String email) {
        Map<String, AttributeValue> key = Map.of(
                "booking_id", new AttributeValue().withS(reservationId),
                "user_email", new AttributeValue().withS(email)
        );
        System.out.println("the key to Check: "+key);
        System.out.println("Before Getting the Reservation");
        GetItemRequest request = new GetItemRequest()
                .withTableName(bookingTable)
                .withKey(key)
                .withProjectionExpression("booking_id");
        Map<String, AttributeValue> result = dynamoDB.getItem(request).getItem();
        System.out.println("After Getting teh reservation_id");

        System.out.println("From does ReservationExist: "+result);

        return result != null && !result.isEmpty();
    }

    public void deleteBookingWithId(String reservationId,String email) {
        Map<String, AttributeValue> key = Map.of(
                "booking_id", new AttributeValue().withS(reservationId),
                "user_email", new AttributeValue().withS(email)
        );

        // Instead of deleting, update the status to "CANCELED"
        Map<String, AttributeValue> attributeValues = new HashMap<>();
        attributeValues.put(":status", new AttributeValue().withS("CANCELLED"));

        Map<String, String> attributeNames = new HashMap<>();
        attributeNames.put("#status", "status");

        UpdateItemRequest updateRequest = new UpdateItemRequest()
                .withTableName(bookingTable)
                .withKey(key)
                .withUpdateExpression("SET #status = :status")
                .withExpressionAttributeNames(attributeNames)
                .withExpressionAttributeValues(attributeValues);

        dynamoDB.updateItem(updateRequest);
    }

    public Map<String, AttributeValue> getBookingByIdAndEmail(String reservationId, String userEmail) {
        Map<String, AttributeValue> key = Map.of(
                "user_email", new AttributeValue().withS(userEmail),
                "booking_id", new AttributeValue().withS(reservationId)
        );

        GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(bookingTable)
                .withKey(key);

        GetItemResult getItemResult = dynamoDB.getItem(getItemRequest);

        return getItemResult.getItem();
    }


    public Map<String,AttributeValue> createBooking(Map<String, AttributeValue> reservationMap) {
        PutItemRequest putItemRequest = new PutItemRequest().withTableName(bookingTable).withItem(reservationMap);
        dynamoDB.putItem(putItemRequest);
        return reservationMap;
    }

    public boolean doesBookingExist(String reservationId) {
        System.out.println("Checking if booking exists with ID: " + reservationId);

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":bookingId", new AttributeValue().withS(reservationId));

        ScanRequest scanRequest = new ScanRequest()
                .withTableName(bookingTable)
                .withFilterExpression("booking_id = :bookingId")
                .withExpressionAttributeValues(expressionAttributeValues)
                .withProjectionExpression("booking_id"); // Only fetch the ID

        ScanResult result = dynamoDB.scan(scanRequest);
        boolean exists = !result.getItems().isEmpty();
        System.out.println("Booking " + (exists ? "found" : "not found") + " for ID: " + reservationId);
        return exists;
    }

    public AttributeValue getAssignedWaiterById(String bookingId){
        Map<String,AttributeValue> key = Map.of("booking_id",new AttributeValue(bookingId));

        QueryRequest queryRequest = new QueryRequest(bookingTable).withKeyConditionExpression("booking_id = :b_id").withExpressionAttributeValues(
                Map.of(":b_id",new AttributeValue(bookingId))
        ).withProjectionExpression("waiter_id");

        return dynamoDB.query(queryRequest).getItems().getFirst().get("waiter_id");
    }

    public Map<String,AttributeValue> getBookingById(String bookingId){
        QueryRequest queryRequest = new QueryRequest(bookingTable).withKeyConditionExpression("booking_id = :b_id")
                .withExpressionAttributeValues(Map.of(
                        ":b_id", new AttributeValue(bookingId)
                ));

        List<Map<String, AttributeValue>> items = dynamoDB.query(queryRequest).getItems();
        return items==null?null:items.getFirst();
    }

    public String getReservationLocationWithId(String bookingId) {
        List<Booking> bookings = getAllBookings();
        String locationId = bookings.stream()
                .filter(booking -> booking.getReservationId().equalsIgnoreCase(bookingId))
                .map(booking -> booking.getLocationId())
                .findFirst()
                .orElse(null);

        if(locationId == null){
            throw new NotFoundException("reservation not found");
        }

        return locationId;
    }

    public List<Booking> getBookingsByIdAndDate(String waiterId, String date) {
        DynamoDbTable<Booking> table = enhancedClient.table(bookingTable, TableSchema.fromBean(Booking.class));
        List<Booking> bookings = new ArrayList<>();
        table.scan().items().forEach(bookings::add);

        System.out.println(bookings);

        return bookings.stream()
                .filter(booking -> booking.getWaiterId().equalsIgnoreCase(waiterId))
                .filter(booking -> booking.getDate().equalsIgnoreCase(date))
                .toList();
    }

    public List<Booking> getAllBookings(){
        DynamoDbTable<Booking> table = enhancedClient.table(bookingTable, TableSchema.fromBean(Booking.class));
        List<Booking> bookings = new ArrayList<>();
        table.scan().items().forEach(bookings::add);

        return bookings;
    }

    public void saveBooking(Booking booking) {
        DynamoDbTable<Booking> table = enhancedClient.table(
                bookingTable,  // <-- replace with your actual table name
                TableSchema.fromBean(Booking.class)
        );

        table.putItem(booking);
    }

    public Booking makeBookingForVisitor(WaiterBookingDTO waiterBookingDTO, String waiterId, String id, String locationId) {
        Booking booking = new Booking();
        booking.setReservationId(id);
        booking.setLocationId(locationId);
        booking.setTableId(waiterBookingDTO.getTableNumber());
        booking.setDate(waiterBookingDTO.getDate());
        booking.setWaiterId(waiterId);
        booking.setTimeFrom(waiterBookingDTO.getTimeFrom());
        booking.setTimeTo(waiterBookingDTO.getTimeTo());
        booking.setGuestsNumber(waiterBookingDTO.getGuestsNumber());
        booking.setUserEmail("dummy");
        booking.setByCustomer(false);
        booking.setStatus("RESERVED");
        booking.setFeedbackId("no_feedback");

        DynamoDbTable<Booking> table = enhancedClient.table(bookingTable, TableSchema.fromBean(Booking.class));
        table.putItem(booking);
        return booking;
    }

    public Booking makeBookingForCustomer(WaiterBookingDTO waiterBookingDTO, String waiterId, String id, String locationId) {
        Booking booking = new Booking();
        booking.setReservationId(id);
        booking.setLocationId(locationId);
        booking.setTableId(waiterBookingDTO.getTableNumber());
        booking.setDate(waiterBookingDTO.getDate());
        booking.setWaiterId(waiterId);
        booking.setTimeFrom(waiterBookingDTO.getTimeFrom());
        booking.setTimeTo(waiterBookingDTO.getTimeTo());
        booking.setGuestsNumber(waiterBookingDTO.getGuestsNumber());
        booking.setUserEmail(waiterBookingDTO.getClientEmail());
        booking.setByCustomer(false);
        booking.setStatus("RESERVED");
        booking.setFeedbackId("no_feedback");

        DynamoDbTable<Booking> table = enhancedClient.table(bookingTable, TableSchema.fromBean(Booking.class));
        table.putItem(booking);
        return booking;
    }

    public void setFeedback(String reservationId, String id) {
        DynamoDbTable<Booking> table = enhancedClient.table(bookingTable, TableSchema.fromBean(Booking.class));
        List<Booking> bookings = new ArrayList<>();
        table.scan().items().forEach(bookings::add);

        Booking booking = bookings.stream()
                .filter(booking1 -> booking1.getReservationId().equalsIgnoreCase(reservationId))
                .findFirst()
                .orElse(null);

        if (booking != null) {
            System.out.println(booking.getFeedbackId());
            booking.setFeedbackId(id);
            table.putItem(booking);
        } else {
            throw new IllegalArgumentException("Booking not found for reservationId: " + reservationId);
        }
    }

    public List<Booking> findBookingsByDate(String date) {

        List<Booking> bookings = getAllBookings();

        return bookings.stream()
                .filter(booking -> booking.getDate().equalsIgnoreCase(date))
                .toList();
    }

    public List<Booking> findBookingByDateAndTime(String inputStartTime, String inputEndTime) {
        try {
            List<Booking> bookings = getAllBookings();

            System.out.println(bookings);
            DateTimeFormatter formatterWithSeconds = DateTimeFormatter.ofPattern("HH:mm:ss");
            DateTimeFormatter formatterWithoutSeconds = DateTimeFormatter.ofPattern("HH:mm");

            LocalTime startTime = LocalTime.parse(inputStartTime, formatterWithSeconds);
            LocalTime endTime = LocalTime.parse(inputEndTime, formatterWithSeconds);
            System.out.println(startTime +" to " + endTime);

            bookings = bookings.stream()
                    .filter(booking -> {
                        LocalTime bookingEndTime = LocalTime.parse(booking.getTimeTo(), formatterWithoutSeconds);
                        return !bookingEndTime.isBefore(startTime) && !bookingEndTime.isAfter(endTime);
                    })
                    .collect(Collectors.toList());
            System.out.println(bookings);
            return bookings;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Date and time parser error", e);
        }
    }
}