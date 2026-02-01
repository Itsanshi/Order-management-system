package com.restaurantback.services;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.restaurantback.dto.reservation.BookingDto;
import com.restaurantback.dto.reservation.ReservationDto;
import com.restaurantback.dto.reservation.WaiterReservationDTO;
import com.restaurantback.exceptions.reservationException.ConflictException;
import com.restaurantback.exceptions.reservationException.ForbiddenException;
import com.restaurantback.exceptions.reservationException.NotFoundException;
import com.restaurantback.exceptions.reservationException.UnauthorizedException;
import com.restaurantback.models.Booking;
import com.restaurantback.models.TimeSlot;
import com.restaurantback.models.User;
import com.restaurantback.repository.BookingRepository;
import com.restaurantback.repository.EmployeeRepository;
import com.restaurantback.utils.TimeslotDB;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.Value;

import javax.validation.ValidationException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BookingService {
    private final BookingRepository bookingRepository;
    private final TableService tableService;
    private final LocationService locationService;
    private final WaiterService waiterService;
    private final EmployeeRepository employeeRepository;
    private final DynamoDbEnhancedClient enhancedClient;

    public BookingService(BookingRepository bookingRepository, TableService tableService, WaiterService waiterService, LocationService locationService, EmployeeRepository employeeRepository, DynamoDbEnhancedClient enhancedClient) {
        this.bookingRepository = bookingRepository;
        this.tableService = tableService;
        this.waiterService = waiterService;
        this.locationService = locationService;
        this.employeeRepository = employeeRepository;
        this.enhancedClient = enhancedClient;
    }

    private static Map<String, AttributeValue> getStringAttributeValueMap(Map<String, AttributeValue> updateFields, Map<String, AttributeValue> existingReservation) {
        Map<String, AttributeValue> updatedBooking = new HashMap<>();
        updatedBooking.put("booking_id", existingReservation.get("booking_id"));
        updatedBooking.put("user_email", existingReservation.get("user_email"));
        updatedBooking.put("locationId", updateFields.getOrDefault("locationId", existingReservation.get("locationId")));
        updatedBooking.put("table_id", updateFields.getOrDefault("tableNumber", existingReservation.get("table_id")));
        updatedBooking.put("date", updateFields.getOrDefault("date", existingReservation.get("date")));
        updatedBooking.put("from", updateFields.getOrDefault("timeFrom", existingReservation.get("from")));
        updatedBooking.put("to", updateFields.getOrDefault("timeTo", existingReservation.get("to")));
        updatedBooking.put("guests", updateFields.getOrDefault("guestsNumber", existingReservation.get("guests")));
        updatedBooking.put("status", existingReservation.get("status"));
        updatedBooking.put("waiter_id", existingReservation.get("waiter_id"));
        updatedBooking.put("feedbackId", existingReservation.get("feedbackId"));
        updatedBooking.put("byCustomer", existingReservation.get("byCustomer"));
        return updatedBooking;
    }

    // Existing methods remain unchanged
    public List<ReservationDto> getListReservationDto(String email) {
        System.out.println("Fetching reservations for email: " + email);
        if (email == null) {
            System.err.println("Authentication failed: email is null");
            throw new UnauthorizedException("Authentication required");
        }

        List<Map<String, AttributeValue>> reservations = bookingRepository.getBookingsByEmail(email);
        System.out.println("Found " + reservations.size() + " reservations for email: " + email);
        return reservations.stream().map(this::toReservationDto).collect(Collectors.toList());
    }

    public ReservationDto toReservationDto(Map<String, AttributeValue> reservation) {
        if (reservation == null) {
            return null;
        }

        ReservationDto reservationDto = new ReservationDto();
        try {
            Function<String, String> locationIdToLocationAddress = locationService::getLocationAddressById;
            System.out.println(reservation);
            reservationDto.setId(getStringValue(reservation, "booking_id"));
            reservationDto.setLocationAddress(locationIdToLocationAddress.apply(getStringValue(reservation, "locationId")));
            reservationDto.setDate(getStringValue(reservation, "date"));
            String timeFrom = getStringValue(reservation, "from");
            String timeTo = getStringValue(reservation, "to");
            reservationDto.setTimeSlot(timeFrom + "-" + timeTo);
            reservationDto.setGuestsNumber(getStringValue(reservation, "guests"));
            reservationDto.setStatus(getStringValue(reservation, "status"));
            reservationDto.setPreOrder(getOptionalStringValue(reservation, "dishes"));
            reservationDto.setFeedbackId(getOptionalStringValue(reservation, "feedbackId"));
            reservationDto.setWaiterName(employeeRepository.getNameFromId(getStringValue(reservation, "waiter_id")));
            reservationDto.setWaiterRating("4.7");

            return reservationDto;
        } catch (Exception e) {
            System.err.println("Error converting reservation to DTO: " + e.getMessage());
            throw new IllegalArgumentException("Invalid reservation data: " + e.getMessage());
        }
    }

    private String getStringValue(Map<String, AttributeValue> map, String key) {
        AttributeValue attr = map.get(key);
        if (attr == null || attr.getS() == null) {
            throw new IllegalArgumentException("Required field missing: " + key);
        }
        return attr.getS();
    }

    private String getOptionalStringValue(Map<String, AttributeValue> map, String key) {
        AttributeValue attr = map.get(key);
        return (attr != null) ? attr.getS() : null;
    }

    public void deleteReservationWithId(String reservationId, String email, boolean isWaiter) {
        System.out.println("reservation_id: " + reservationId + " email: " + email + " in deleteMethod");

        if (email == null) {
            throw new UnauthorizedException("Authentication required");
        }

        // Get reservation based on access type
        Map<String, AttributeValue> reservation = null;
        if (isWaiter) {
            if (!bookingRepository.doesBookingExist(reservationId))
                throw new NotFoundException("The Reservation with id: " + reservationId + " does not exists!");
            reservation = bookingRepository.getBookingById(reservationId);
        } else {
            verifyUserAccess(reservationId, email);
            reservation = bookingRepository.getBookingByIdAndEmail(reservationId, email);
        }

        validateReservationStatus(reservation);

        if (!isWaiter) {
            validateCancellationTimeLimit(reservation);
        }

        processCancellation(reservation);
    }

    private void validateReservationStatus(Map<String, AttributeValue> reservation) {
        if ("CANCELLED".equals(reservation.get("status").getS())) {
            throw new ValidationException("Reservation is already cancelled");
        }
    }

    private void validateCancellationTimeLimit(Map<String, AttributeValue> reservation) {
        String reservationDate = reservation.get("date").getS();
        String reservationTimeFrom = reservation.get("from").getS();

        if (!canCancelReservation(reservationDate, reservationTimeFrom)) {
            throw new ValidationException("Reservations can only be canceled up to 30 minutes before the reservation time.");
        }
    }

    private void verifyUserAccess(String reservationId, String userEmail) {
        System.out.println("reservation_id: " + reservationId + " email: " + userEmail + " in VerifyUserAccess Method");

        if (!bookingRepository.doesBookingExistForUserWithEmailAndId(reservationId, userEmail)) {
            throw new UnauthorizedException("User can only cancel their own reservations");
        }
    }

    private void processCancellation(Map<String, AttributeValue> reservation) {
        try {
            System.out.println("IN processCancellation reservationMap: " + reservation);
            String reservationId = reservation.get("booking_id").getS();
            String tableId = reservation.get("table_id").getS();
            String locationId = reservation.get("locationId").getS();
            String waiterId = reservation.get("waiter_id").getS();
            String reservationDate = reservation.get("date").getS();
            String timeSlot = reservation.get("from").getS() + "-" + reservation.get("to").getS();
            System.out.println("before email");
            String email = reservation.get("user_email").getS();
            System.out.println("after email");

            // 1. Cancel the reservation
            bookingRepository.deleteBookingWithId(reservationId, email);
            System.out.println("After deleteReservationWithId");

            // 2. Remove time slot from table's schedule
            removeTimeSlotFromTable(tableId, locationId, reservationDate, timeSlot);
            System.out.println("after table time slot removal");

            // 3. Remove time slot from waiter's schedule
            removeTimeSlotFromWaiter(waiterId, locationId, reservationDate, timeSlot);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to cancel reservation: " + e.getMessage());
        }
    }

    private void verifyBookingForUserWithEmail(String reservationId, String email) {
        // Get reservation details before deletion
        if (!bookingRepository.doesBookingExist(reservationId)) {
            throw new NotFoundException("Reservation not found");
        }

        if (!bookingRepository.doesBookingExistForUserWithEmailAndId(reservationId, email)) {
            throw new ForbiddenException("You can only cancel your own reservations");
        }
    }

    private void removeTimeSlotFromTable(String tableId, String locationId, String date, String timeSlot) {
        try {
            Map<String, AttributeValue> tableData = tableService.getTableDataById(tableId, locationId);
            if (tableData != null && tableData.containsKey("booked")) {
                // Remove the specific time slot
                // You'll need to implement this in TableService/Repository
                tableService.removeTimeSlot(tableId, locationId, date, timeSlot);
            }
        } catch (Exception e) {
            System.err.println("Error updating table schedule: " + e.getMessage());
            throw new ValidationException("Failed to update table schedule");
        }
    }

    private void removeTimeSlotFromWaiter(String waiterId, String locationId, String date, String timeSlot) {
        try {
            // Remove the specific time slot from waiter's schedule
            // You'll need to implement this in WaiterService/Repository
            waiterService.removeTimeSlot(waiterId, locationId, date, timeSlot);
        } catch (Exception e) {
            System.err.println("Error updating waiter schedule: " + e.getMessage());
            throw new ValidationException("Failed to update waiter schedule");
        }
    }

    private boolean canCancelReservation(String date, String timeFrom) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime reservationDateTime = LocalDateTime.parse(date + "T" + timeFrom, formatter);

            LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).truncatedTo(ChronoUnit.MINUTES);

            System.out.println("Reservation time: " + reservationDateTime);
            System.out.println("Current time (IST): " + currentTime);

            long minutesUntilReservation = ChronoUnit.MINUTES.between(currentTime, reservationDateTime);
            return minutesUntilReservation > 30;
        } catch (Exception e) {
            System.err.println("Error parsing date/time: " + e.getMessage());
            return false;
        }
    }

    // New methods for existence and logical validation
    public void validateExistenceAndLogicalConstraints(BookingDto booking) {
        // TC059: Check required fields
        if (booking == null) {
            throw new ValidationException("Booking details are required");
        }

        validateRequiredFields(booking);
        validateBasicValues(booking);
        validateDateFormat(booking.getDate());
        validateTableAndLocationExistence(booking);
        validateTableLogicalConstraints(booking);
    }

    private void validateRequiredFields(BookingDto booking) {
        System.out.println("Validating required fields for booking");
        List<String> missingFields = new ArrayList<>();

        if (booking.getLocationId() == null) missingFields.add("locationId");
        if (booking.getGuestsNumber() == null) missingFields.add("guestsNumber");
        if (booking.getDate() == null) missingFields.add("date");
        if (booking.getTimeFrom() == null) missingFields.add("timeFrom");
        if (booking.getTimeTo() == null) missingFields.add("timeTo");

        if (!missingFields.isEmpty()) {
            System.err.println("Missing required fields in booking request: " + missingFields);
            throw new ValidationException("Missing required fields: " + String.join(", ", missingFields));
        }
        System.out.println("All required fields present");
    }

    private void validateDateFormat(String date) {
        try {
            LocalDate.parse(date); // TC057
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format. Use YYYY-MM-DD");
        }
    }

    private void validateTableAndLocationExistence(BookingDto booking) {
        if (!locationService.doesLocationExist(booking.getLocationId())) {
            throw new NotFoundException("Location not found: " + booking.getLocationId());
        }

        if (!tableService.doesTableExistsWithIdAndLocationId(booking.getTableId(), booking.getLocationId())) {
            throw new NotFoundException("Table not found: " + booking.getTableId());
        }
    }

    private void validateTableLogicalConstraints(BookingDto booking) {
        System.out.println("Validating logical constraints for booking");

        if (!tableService.isTableAvailableForBooking(booking)) {
            System.err.println("Table double booking attempt - Table: " + booking.getTableId() + ", Date: " + booking.getDate() + ", Time: " + booking.getTimeFrom() + "-" + booking.getTimeTo());
            throw new ConflictException("Table is already booked for this time slot");
        }

        tableService.verifyTableCanAccommodateGuestsWithIdAndLocationId(booking.getTableId(), booking.getLocationId(), booking.getGuestsNumber());


        System.out.println("Logical constraints validation passed");
    }

    public void validateBasicValues(BookingDto booking) {
        try {
            validateGuestCount(booking.getGuestsNumber());
            validateDateTime(booking.getDate(), booking.getTimeFrom(), booking.getTimeTo());
        } catch (Exception e) {
            throw new ValidationException("Invalid booking details: " + e.getMessage());
        }
    }

    private void validateGuestCount(String guestsNumber) {
        int guests = Integer.parseInt(guestsNumber);
        if (guests < 1 || guests > 20) {
            throw new ValidationException("Invalid party size (must be between 1 and 20)");
        }
    }

    private void validateDateTime(String date, String timeFrom, String timeTo) {
        validateDateFormat(date);
        validateTime(timeFrom);
        validateTime(timeTo);
        LocalDate bookingDate = LocalDate.parse(date);
        LocalTime bookingTime = LocalTime.parse(timeFrom);
        LocalTime endTime = LocalTime.parse(timeTo);

        if (bookingTime.isAfter(endTime) || bookingTime.equals(endTime)) {
            throw new ValidationException("End time must be after start time");
        }

        if (bookingDate.atTime(bookingTime).isBefore(LocalDate.now().atTime(LocalTime.now()))) {
            throw new ValidationException("Cannot book for past date/time");
        }
    }

    private void validateTime(String time) {
        try {
            LocalTime.parse(time);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Enter a valid Time in the Format HH:MM! Your input: " + time);
        }
    }

    public ReservationDto createReservation(BookingDto booking, String reservationId) {
        System.out.println("bookingDto: " + booking);

        // Validate business rules
        validateExistenceAndLogicalConstraints(booking);

        // Assign waiter
        String waiterId = waiterService.getLeastBusyWaiter(booking.getLocationId(), booking.getDate(), booking.getTimeFrom(), booking.getTimeTo());
        System.out.println("Assigned waiter ID: " + waiterId);

        Map<String, AttributeValue> reservationMap = new HashMap<>();
        reservationMap.put("booking_id", new AttributeValue().withS(reservationId));
        reservationMap.put("user_email", new AttributeValue().withS(booking.getUserEmail()));
        reservationMap.put("table_id", new AttributeValue().withS(booking.getTableId()));
        reservationMap.put("locationId", new AttributeValue().withS(booking.getLocationId()));
        reservationMap.put("date", new AttributeValue().withS(booking.getDate()));
        reservationMap.put("from", new AttributeValue().withS(booking.getTimeFrom()));
        reservationMap.put("to", new AttributeValue().withS(booking.getTimeTo()));
        reservationMap.put("guests", new AttributeValue().withS(booking.getGuestsNumber()));
        reservationMap.put("waiter_id", new AttributeValue().withS(waiterId));
        reservationMap.put("status", new AttributeValue().withS("RESERVED"));
        reservationMap.put("byCustomer", new AttributeValue().withBOOL(true));
        reservationMap.put("feedbackId", new AttributeValue().withS("no_feedback"));

        // Update waiter schedule
        System.out.println("Updating waiter schedule - Waiter: " + waiterId);
        updateWaiterSchedule(waiterId, booking);

        // Update table schedule
        System.out.println("Updating table schedule - Table: " + booking.getTableId());
        updateTableSchedule(booking);

        System.out.println("Reservation data: " + reservationMap);
        Map<String, AttributeValue> reservation = bookingRepository.createBooking(reservationMap);
        System.out.println("Successfully created reservation: " + reservationId);


        System.out.println("Completed all updates for reservation: " + reservationId);
        return toReservationDto(reservation);
    }

    private void updateWaiterSchedule(String waiterId, BookingDto booking) {
        try {
            waiterService.updateTimeSlotForWaiterWithIdAndLocationId(waiterId, booking.getLocationId(), booking.getDate(), booking.getTimeFrom() + "-" + booking.getTimeTo());
        } catch (Exception e) {
            System.err.println("Error updating waiter schedule: " + e.getMessage());
            throw new RuntimeException("Failed to update waiter schedule for waiter: " + waiterId, e);
        }
    }

    private void updateTableSchedule(BookingDto booking) {
        try {
            tableService.updateTimeSlotForTableWithIdAndLocationId(booking.getTableId(), booking.getLocationId(), booking.getDate(), booking.getTimeFrom() + "-" + booking.getTimeTo());
        } catch (Exception e) {
            System.err.println("Error updating table schedule: " + e.getMessage());
            throw new RuntimeException("Failed to update table schedule for table: " + booking.getTableId(), e);
        }
    }

    public ReservationDto updateBookingDateAndTimeWithId(String bookingId, String userEmail, Map<String, String> bodyMap, boolean isWaiter) {

        // Convert to Map<String,AttributeValue>
        Map<String, AttributeValue> updateFields = new HashMap<>();
        for (Map.Entry<String, String> entry : bodyMap.entrySet()) {
            updateFields.put(entry.getKey(), new AttributeValue(entry.getValue()));
        }

        String timeFrom = updateFields.get("timeFrom").getS();
        String timeTo = updateFields.get("timeTo").getS();

        TimeSlot timeSlot = parseTimeSlot(timeFrom);
        if(timeSlot == null || !timeSlot.getTo().equalsIgnoreCase(timeTo)){
            throw new NotFoundException("TimeSlot not found");
        }

        try {
            if (updateFields.isEmpty())
                throw new IllegalArgumentException("The updation process must have at least one updation!");
            if (!isWaiter) verifyBookingForUserWithEmail(bookingId, userEmail);
            if (!bookingRepository.doesBookingExist(bookingId)) {
                throw new NotFoundException("Reservation not found");
            }
            if (updateFields.containsKey("timeFrom") ^ updateFields.containsKey("timeTo"))
                throw new IllegalArgumentException("timeFrom and timeTo must be updated together");

            Map<String, AttributeValue> existingReservation = null;

            if (!isWaiter) existingReservation = bookingRepository.getBookingByIdAndEmail(bookingId, userEmail);
            else existingReservation = bookingRepository.getBookingById(bookingId);

            System.out.printf("Existing Reservation: %s", existingReservation);

            validateReservationStatus(existingReservation);

            // Prevent locationID and tableNumber changes
            if (updateFields.containsKey("locationId") || updateFields.containsKey("tableNumber")) {
                throw new ValidationException("Cannot change location or table for an existing booking\nTo change the location, please cancel and rebook");
            }

            Map<String, AttributeValue> updatedBooking = getStringAttributeValueMap(updateFields, existingReservation);
            System.out.printf("The merged data to be inserted into the table: %s", updatedBooking);

            BookingDto bookingDto = convertToBookingDto(updatedBooking);
            System.out.printf("updated Booking Dto: %s\n", bookingDto);

            ReservationDto reservationDto = null;

            if (updateFields.containsKey("date") || updateFields.containsKey("timeFrom") || updateFields.containsKey("timeTo")) {
                removeTimeSlotFromTable(existingReservation.get("table_id").getS(), existingReservation.get("locationId").getS(), existingReservation.get("date").getS(), existingReservation.get("from").getS() + "-" + existingReservation.get("to").getS());
                removeTimeSlotFromWaiter(existingReservation.get("waiter_id").getS(), existingReservation.get("locationId").getS(), existingReservation.get("date").getS(), existingReservation.get("from").getS() + "-" + existingReservation.get("to").getS());

                try {
                    if (isWaiter) {
                        if (!tableService.isTableAvailableForBooking(bookingDto)) {
                            throw new ConflictException("Table not available for new time slot");
                        }
                        if (!waiterService.isWaiterAvailableByIdAndLocationIdForDateAndTime(existingReservation.get("waiter_id").getS(), bookingDto.getLocationId(), bookingDto.getDate(), bookingDto.getTimeFrom(), bookingDto.getTimeTo())) {
                            throw new ConflictException("Waiter is not available for new time slot and date");
                        }
                        updateTableSchedule(bookingDto);
                        updateWaiterSchedule(existingReservation.get("waiter_id").getS(), bookingDto);
                        reservationDto = toReservationDto(bookingRepository.createBooking(updatedBooking));
                    } else {
                        reservationDto = createReservation(bookingDto, bookingId);
                    }
                } catch (Exception e) {
                    updateTableSchedule(convertToBookingDto(existingReservation));
                    updateWaiterSchedule(existingReservation.get("waiter_id").getS(), convertToBookingDto(existingReservation));
                    throw e;
                }
            } else if (updateFields.containsKey("guestsNumber")) {
                tableService.verifyTableCanAccommodateGuestsWithIdAndLocationId(bookingDto.getTableId(), bookingDto.getLocationId(), updateFields.get("guestsNumber").getS());
                reservationDto = toReservationDto(bookingRepository.createBooking(updatedBooking));
            } else
                throw new IllegalArgumentException("Enter valid Fields to update the Booking! : " + updateFields.keySet().stream().collect(Collectors.joining(",", "[", "]")));

            return reservationDto;

        } catch (Exception e) {
            System.err.println("Error occurred while updating booking: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private BookingDto convertToBookingDto(Map<String, AttributeValue> updatedBooking) {
        BookingDto bookingDto = new BookingDto();
        bookingDto.setReservation_id(updatedBooking.get("booking_id").getS());
        bookingDto.setLocationId(updatedBooking.get("locationId").getS());
        bookingDto.setTableId(updatedBooking.get("table_id").getS());
        bookingDto.setDate(updatedBooking.get("date").getS());
        bookingDto.setTimeFrom(updatedBooking.get("from").getS());
        bookingDto.setTimeTo(updatedBooking.get("to").getS());
        bookingDto.setGuestsNumber(updatedBooking.get("guests").getS());
        bookingDto.setUserEmail(updatedBooking.get("user_email").getS()); // Set manually
        bookingDto.setWaiterId(updatedBooking.get("waiter_id").getS());
        bookingDto.setFeedbackId(updatedBooking.get("feedbackId").getS());
        bookingDto.setByCustomer(updatedBooking.get("byCustomer").getBOOL());
        return bookingDto;
    }

    public ReservationDto updateBookingTableWithId(String bookingId, String tableId, String locationId) {
        if (!bookingRepository.doesBookingExist(bookingId)) {
            throw new NotFoundException("Reservation not found");
        }

        // Get existing booking
        Map<String, AttributeValue> repository = bookingRepository.getBookingById(bookingId);
        BookingDto bookingDto = convertToBookingDto(repository);

        // Store old table info for later removal
        String oldTableId = bookingDto.getTableId();
        String oldLocationId = bookingDto.getLocationId();

        // Update the booking DTO with new table ID and location ID
        bookingDto.setTableId(tableId);
        bookingDto.setLocationId(locationId);

        // Now validate with the NEW table ID and location ID
        validateTableAndLocationExistence(bookingDto);
        validateTableLogicalConstraints(bookingDto);

        // Remove time slot from old table
        removeTimeSlotFromTable(oldTableId, oldLocationId,
                bookingDto.getDate(), bookingDto.getTimeFrom() + "-" + bookingDto.getTimeTo());

        // Update table schedule with new table
        updateTableSchedule(bookingDto);

        // Update repository with new values
        repository.put("table_id", new AttributeValue(tableId));
        repository.put("locationId", new AttributeValue(locationId));  // Note: case sensitive key

        return toReservationDto(bookingRepository.createBooking(repository));
    }

    public List<WaiterReservationDTO> getWaiterReservation(String email, String date, String time, String tableNumber) {
        String waiterID = employeeRepository.getWaiterIdFromEmail(email);
        System.out.println("Waiter id = " + waiterID);
        System.out.println(date);
        List<Booking> bookings1 = bookingRepository.getBookingsByIdAndDate(waiterID, date);
        System.out.println(bookings1);

        List<Booking> bookings = filterBookingByTimeAndTable(bookings1, time, tableNumber);
        System.out.println(bookings);

        List<WaiterReservationDTO> waiterReservationDTOList = new ArrayList<>();
        for (Booking booking : bookings) {
            String doneBy;
            if (booking.isByCustomer()) {
                doneBy = "Customer " + getNameByEmail(booking.getUserEmail());
            } else {
                doneBy = "Waiter " + getNameByEmail(email);
            }

            WaiterReservationDTO waiterReservationDTO = new WaiterReservationDTO(locationService.getLocationAddressById(booking.getLocationId()), booking.getTableId(), date, booking.getTimeFrom() + "-" + booking.getTimeTo(), doneBy, booking.getGuestsNumber(), booking.getReservationId(), booking.getStatus(), waiterID);
            waiterReservationDTOList.add(waiterReservationDTO);
        }

        return waiterReservationDTOList;
    }

    private List<Booking> filterBookingByTimeAndTable(List<Booking> bookings, String time, String tableNumber) {
        String startTime = time != null ? time.split("-")[0] : null;

        return bookings.stream().filter(booking -> startTime == null || booking.getTimeFrom().equalsIgnoreCase(startTime)).filter(booking -> tableNumber == null || booking.getTableId().equalsIgnoreCase(tableNumber)).toList();
    }


    private String getNameByEmail(String userEmail) {
        String name = System.getenv("userTable");
        DynamoDbTable<User> table = enhancedClient.table(name, TableSchema.fromBean(User.class));
        List<User> users = new ArrayList<>();
        table.scan().items().forEach(users::add);
        User user = users.stream().filter(user1 -> user1.getEmail().equalsIgnoreCase(userEmail)).findFirst().orElse(null);
        assert user != null;
        return user.getFirstName() + user.getLastName();
    }

    public void updateBookings() {
        List<Booking> bookings = bookingRepository.getAllBookings();

        System.out.println(bookings);

        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        LocalDateTime now = ZonedDateTime.now(zoneId).toLocalDateTime();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Booking booking : bookings) {
            try {
                LocalDate bookingDate = LocalDate.parse(booking.getDate(), dateFormatter);
                LocalTime fromTime = LocalTime.parse(booking.getTimeFrom(), timeFormatter);
                LocalTime toTime = LocalTime.parse(booking.getTimeTo(), timeFormatter);
                LocalDateTime fromDateTime = LocalDateTime.of(bookingDate, fromTime);
                LocalDateTime toDateTime = LocalDateTime.of(bookingDate, toTime);

                System.out.println(fromTime);
                System.out.println(toTime);

                System.out.println(now);
                // Logic to update status
                if (toDateTime.isBefore(now) && !booking.getStatus().equalsIgnoreCase("Cancelled")) {
                    booking.setStatus("FINISHED");
                } else if (!now.isBefore(fromDateTime) && !now.isAfter(toDateTime) && !booking.getStatus().equalsIgnoreCase("Cancelled")) {
                    booking.setStatus("IN PROGRESS");
                } else {
                    continue; // Not finished or in-progress; skip update
                }

                System.out.println(booking);

                // Save updated booking
                bookingRepository.saveBooking(booking); // or putItem() if you're doing it manually

            } catch (Exception e) {
                System.err.println("Error parsing date/time for booking ID: " + booking.getReservationId());
                e.printStackTrace();
            }
        }
    }

    private TimeSlot parseTimeSlot(String timeSlotString) {
        System.out.println(timeSlotString);


        System.out.println("Converting to Entity");
        // Fetch TimeSlot from DB to get the ID
        TimeSlot fetchedTimeSlot = TimeslotDB.getTimeSlotFromDB(timeSlotString);

        System.out.println(fetchedTimeSlot);
        return fetchedTimeSlot;
    }
}
