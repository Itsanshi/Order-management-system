package com.restaurantback.services;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.restaurantback.exceptions.reservationException.NotFoundException;
import com.restaurantback.repository.WaiterRepository;

import javax.validation.ValidationException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class WaiterService {
    WaiterRepository waiterRepository;

    public WaiterService(WaiterRepository waiterRepository) {
        this.waiterRepository = waiterRepository;
    }

    public String getLeastBusyWaiter(String locationId, String date, String timeFrom, String timeTo) {
        System.out.println("Finding least busy waiter for location: " + locationId);

        if (locationId == null || date == null || timeFrom == null || timeTo == null) {
            throw new IllegalArgumentException("All parameters are required");
        }

        List<Map<String, AttributeValue>> waiters = waiterRepository.getWaitersByLocation(locationId);
        System.out.println(waiters);

        if (waiters.isEmpty()) {
            throw new ValidationException("No waiters available for the selected location");
        }

        List<Map<String, AttributeValue>> eligibleWaiters = waiters.stream().filter(waiter -> isWaiterAvailable(waiter, timeFrom, timeTo, date)).collect(Collectors.toList());
        System.out.println(eligibleWaiters);

        if (eligibleWaiters.isEmpty()) {
            throw new ValidationException("No eligible waiters available for the selected time slot");
        }

        try {
            Map<String, Double> waiterScores = calculateWaiterScores(eligibleWaiters, date, timeFrom, timeTo);
            System.out.println(waiterScores);

            return waiterScores.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow(() -> new ValidationException("No suitable waiter found")).getKey();
        } catch (Exception e) {
            System.err.println("Error calculating waiter scores: " + e.getMessage());
            throw new ValidationException("Failed to find suitable waiter: " + e.getMessage());
        }
    }

    private Map<String, Double> calculateWaiterScores(List<Map<String, AttributeValue>> waiters, String date, String timeFrom, String timeTo) {
        Map<String, Double> scores = new HashMap<>();

        for (Map<String, AttributeValue> waiter : waiters) {
            String waiterId = waiter.get("waiter_id").getS();
            List<String> bookedSlots = getBookedTimeSlotsForDate(waiter, date);
            double score = calculateClumsinessScore(bookedSlots, timeFrom, timeTo);
            scores.put(waiterId, score);
        }

        return scores;
    }

    private boolean isWaiterAvailable(Map<String, AttributeValue> waiter, String timeFrom, String timeTo, String date) {
        String shiftStart = waiter.get("shift_start").getS();
        String shiftEnd = waiter.get("shift_end").getS();

        // Check if within shift
        boolean withinShift = timeFrom.compareTo(shiftStart) >= 0 && timeTo.compareTo(shiftEnd) <= 0;

        if (!withinShift) return false;

        // Now check if already booked
        List<String> bookedSlots = getBookedTimeSlotsForDate(waiter, date); // You'll need to pass date here
        for (String slot : bookedSlots) {
            String[] parts = slot.split("-");
            String bookedFrom = parts[0];
            String bookedTo = parts[1];

            // Check for overlap
            if (timeFrom.compareTo(bookedTo) < 0 && timeTo.compareTo(bookedFrom) > 0) {
                return false;
            }
        }

        return true;
    }

    public void updateTimeSlotForWaiterWithIdAndLocationId(String waiterId, String locationId, String date, String timeslot) {
        System.out.println("Updating time slot for waiter: " + waiterId);

        if (waiterId == null || locationId == null || date == null || timeslot == null) {
            throw new IllegalArgumentException("All parameters are required");
        }

        if (!doesWaiterExistWithIdAndLocationId(waiterId,locationId)) {
            throw new NotFoundException("Waiter not found");
        }

        try {
            waiterRepository.updateTimeSlotForWaiterWithId(waiterId, locationId, date, timeslot);
            System.out.println("Successfully updated time slot for waiter: " + waiterId);
        } catch (Exception e) {
            System.err.println("Failed to update waiter time slot: " + e.getMessage());
            throw new ValidationException("Failed to update waiter schedule: " + e.getMessage());
        }
    }

    private List<String> getBookedTimeSlotsForDate(Map<String, AttributeValue> waiter, String date) {
        AttributeValue bookedTimeSlotsAttr = waiter.get("booked");
        if (bookedTimeSlotsAttr == null || bookedTimeSlotsAttr.getM() == null) {
            return new ArrayList<>();
        }

        Map<String, AttributeValue> bookedTimeSlots = bookedTimeSlotsAttr.getM();
        AttributeValue dateSlots = bookedTimeSlots.get(date);
        if (dateSlots == null || dateSlots.getL() == null) {
            return new ArrayList<>();
        }

        return dateSlots.getL().stream().map(AttributeValue::getS).collect(Collectors.toList());
    }

    private double calculateClumsinessScore(List<String> bookedTimeSlots, String timeFrom, String timeTo) {
        // Sort the booked time slots
        bookedTimeSlots.sort(Comparator.naturalOrder());

        // Find the last booking before the requested time slot
        String lastBooking = bookedTimeSlots.stream().filter(slot -> slot.split("-")[1].compareTo(timeFrom) <= 0).reduce((first, second) -> second) // Get the last one
                .orElse(null);

        // Find the next booking after the requested time slot
        String nextBooking = bookedTimeSlots.stream().filter(slot -> slot.split("-")[0].compareTo(timeTo) >= 0).findFirst().orElse(null);

        // Calculate gaps
        double gapBefore = (lastBooking != null) ? calculateTimeDifference(lastBooking.split("-")[1], timeFrom) : Double.MAX_VALUE; // No booking before, infinite gap

        double gapAfter = (nextBooking != null) ? calculateTimeDifference(timeTo, nextBooking.split("-")[0]) : Double.MAX_VALUE; // No booking after, infinite gap

        return gapBefore + gapAfter;
    }

    private double calculateTimeDifference(String time1, String time2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime t1 = LocalTime.parse(time1, formatter);
        LocalTime t2 = LocalTime.parse(time2, formatter);

        return Duration.between(t1, t2).toMinutes();
    }

    public void removeTimeSlot(String waiterId, String locationId, String date, String timeSlot) {
        waiterRepository.removeTimeSlotFromWaiter(waiterId,locationId,date,timeSlot);
    }

    public boolean doesWaiterExistWithIdAndLocationId(String waiterId,String locationId){
        return waiterRepository.doesWaiterExistWithIdAndLocationId(waiterId,locationId);
    }

    public boolean isWaiterAvailableByIdAndLocationIdForDateAndTime(String waiterId,String locationId,String date,String timeFrom,String timeTo){
        Map<String,AttributeValue> waiter = waiterRepository.getWaiterByIdAndLocationId(waiterId,locationId);
        return isWaiterAvailable(waiter,timeFrom,timeTo,date);

    }
}
