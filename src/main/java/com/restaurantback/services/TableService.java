package com.restaurantback.services;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.restaurantback.dto.TableAvailableDTO;
import com.restaurantback.dto.reservation.BookingDto;
import com.restaurantback.exceptions.reservationException.ConflictException;
import com.restaurantback.exceptions.reservationException.NotFoundException;
import com.restaurantback.models.Location;
import com.restaurantback.models.Table;
import com.restaurantback.models.TimeSlot;
import com.restaurantback.repository.TableRepository;
import com.restaurantback.utils.TableFilter;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import javax.validation.ValidationException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TableService {

    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;
    private final LocationService locationService;
    private final TableRepository tableRepository;

    public TableService(DynamoDbEnhancedClient dynamoDbEnhancedClient, LocationService locationService, TableRepository tableRepository) {
        this.enhancedClient = dynamoDbEnhancedClient;
        this.tableName = System.getenv("tablesTable");
        this.locationService = locationService;
        this.tableRepository = tableRepository;
    }

    public List<TableAvailableDTO> getAvailableTables(String locationId, Date date, TimeSlot timeSlot, int guests) {
        System.out.println(locationId + " " + date + " " + timeSlot + " " + guests);
        List<Table> allTables = fetchTablesByLocation(locationId);
        System.out.println(allTables);
        if(allTables == null || allTables.isEmpty()){
            return null;
        }
        String imageUrl = fetchImageUrlByLocation(locationId);
        return feedData(allTables, date, imageUrl);
    }

    private String fetchImageUrlByLocation(String locationId) {
        Location location = locationService.getLocationById(locationId);
        return location.getImage();
    }

    private List<Table> fetchTablesByLocation(String locationId) {
        DynamoDbTable<Table> table = enhancedClient.table(tableName, TableSchema.fromBean(Table.class));
        List<Table> tables = new ArrayList<>();
        table.scan().items().forEach(tables::add);

        System.out.println(tables);

        return tables.stream()
                .filter(table1 -> locationId.equalsIgnoreCase(table1.getLocationId()))
                .toList();
    }

    private List<TableAvailableDTO> feedData(List<Table> tables, Date date, String imageUrl){
        List<TableAvailableDTO> availableDTOS = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        String dateString = formatter.format(date);
        for(Table table : tables){
            TableAvailableDTO availableDTO = new TableAvailableDTO();
            availableDTO.setLocationId(table.getLocationId());
            availableDTO.setCapacity(Integer.parseInt(table.getCapacity()));
            availableDTO.setTableNumber(table.getId());
            availableDTO.setLocationAddress(locationService.getLocationAddressByLocationId(table.getLocationId()));
            List<String> availableTime = TableFilter.getAvailableTimeSlot(table.getBooked().get(dateString), dateString);
            System.out.println(availableTime);
            availableDTO.setAvailableSlots(availableTime);
            availableDTO.setImageUrl(imageUrl);

            availableDTOS.add(availableDTO);
        }

        return availableDTOS;
    }


    private boolean isTimeSlotAvailable(List<String> existingSlots, String timeFrom, String timeTo) {
        System.out.println("[isTimeSlotAvailable] Checking " + timeFrom + "-" + timeTo);

        for (String slot : existingSlots) {
            String[] times = slot.split("-");
            System.out.println("[isTimeSlotAvailable] Comparing with: " + slot);

            if (timeFrom.compareTo(times[1]) < 0 && timeTo.compareTo(times[0]) > 0) {
                System.out.println("[isTimeSlotAvailable] Conflict detected");
                return false;
            }
        }
        System.out.println("[isTimeSlotAvailable] No conflicts");
        return true;
    }

    public void updateTimeSlotForTableWithIdAndLocationId(
            String tableId, String locationId, String date, String timeslot) {

        System.out.println("[updateTimeSlot] Attempting update for table " + tableId);

        try {
            if (!isTimeSlotAvailable(
                    tableRepository.getBookedTimeSlots(tableId, locationId).getOrDefault(date, List.of()),
                    timeslot.split("-")[0],
                    timeslot.split("-")[1]
            )) {
                System.out.println("[updateTimeSlot] Duplicate booking detected");
                throw new ConflictException("Table already booked");
            }

            tableRepository.updateTimeSlotForTableWithId(tableId, locationId, date, timeslot);
            System.out.println("[updateTimeSlot] Successfully updated");

        } catch (Exception e) {
            System.err.println("[updateTimeSlot] Error: " + e.getMessage());
            throw e;
        }
    }

    public boolean doesTableExist(String tableId, String locationId) {
        System.out.println("[doesTableExist] Checking table " + tableId);
        return tableRepository.doesTableExist(tableId, locationId);
    }

    public Map<String, AttributeValue> getTableDataById(String tableId, String locationId) {
        System.out.println("[getTableData] Fetching data for table " + tableId);
        Map<String, AttributeValue> data = tableRepository.getTableDataById(tableId, locationId);
        if (data == null) {
            System.out.println("[getTableData] Table not found");
            throw new NotFoundException("Table not found");
        }
        return data;
    }

    public void removeTimeSlot(String tableId, String locationId, String date, String timeSlot) {
        System.out.println("[removeTimeSlot] Removing slot " + timeSlot + " from table " + tableId);
        tableRepository.removeTimeSlotFromTable(tableId, locationId, date, timeSlot);
        System.out.println("[removeTimeSlot] Successfully removed");
    }

    public boolean isTableAvailableForBooking(BookingDto booking) {
        System.out.println("[isTableAvailable] Checking booking: " + booking);

        try {
            if (!doesTableExistsWithIdAndLocationId(booking.getTableId(), booking.getLocationId())) {
                System.out.println("[isTableAvailable] Table not found");
                return false;
            }

            Map<String, List<String>> bookedTimeSlots = tableRepository.getBookedTimeSlots(
                    booking.getTableId(),
                    booking.getLocationId()
            );

            if (bookedTimeSlots.isEmpty() || !bookedTimeSlots.containsKey(booking.getDate())) {
                System.out.println("[isTableAvailable] No bookings for this date");
                return true;
            }

            boolean available = isTimeSlotAvailable(
                    bookedTimeSlots.get(booking.getDate()),
                    booking.getTimeFrom(),
                    booking.getTimeTo()
            );
            System.out.println("[isTableAvailable] Slot available: " + available);
            return available;

        } catch (Exception e) {
            System.err.println("[isTableAvailable] Error: " + e.getMessage());
            throw new ValidationException("Availability check failed");
        }
    }



    public boolean doesTableExistsWithIdAndLocationId(String tableId, String locationId) {
        System.out.println("[doesTableExist] Checking table " + tableId);
        return tableRepository.doesTableExistsWithIdAndLocationId(tableId, locationId);
    }



    public void verifyTableCanAccommodateGuestsWithIdAndLocationId(String tableId, String locationId, String guests) {
        String tableCapacity = getNumberOfGuestsForTableWithIdAndLocationId(tableId, locationId);
        try{
            System.out.printf("Before conversion = guestNumber: %s,capacity: %s .",guests,tableCapacity);

            int guestCount = Integer.parseInt(guests);
            int capacity = Integer.parseInt(tableCapacity);
            System.out.printf("After conversion = guestNumber: %d,capacity: %d .",guestCount,capacity);

            if (guestCount <= capacity) {
            } else {
                throw new ValidationException(
                        "Table cannot accommodate " + guests + " guests. Maximum capacity is " + tableCapacity);
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for guests or table capacity");
        }
    }

    private String getNumberOfGuestsForTableWithIdAndLocationId(String tableId, String locationId){
        AttributeValue numberOfGuestsForTableWithIdAndLocationId = tableRepository.getNumberOfGuestsForTableWithIdAndLocationId(tableId, locationId);
        System.out.printf("guestNumber: %s",numberOfGuestsForTableWithIdAndLocationId);
        if(numberOfGuestsForTableWithIdAndLocationId==null)throw new RuntimeException("The Table does not contain the capacity Attribute");
        System.out.printf("getS: %s, getN: %s",numberOfGuestsForTableWithIdAndLocationId.getS(),numberOfGuestsForTableWithIdAndLocationId.getN());
        return numberOfGuestsForTableWithIdAndLocationId.getN();
    }
}

