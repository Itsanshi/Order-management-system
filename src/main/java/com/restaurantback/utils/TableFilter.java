package com.restaurantback.utils;

import com.restaurantback.models.Table;
import com.restaurantback.models.TimeSlot;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.Value;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TableFilter {
    public static List<Table> filterTables(List<Table> tables, Date date, int guests, TimeSlot timeSlot) {
        String timeSlotKey = timeSlot.getFrom() + "-" + timeSlot.getTo();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        String dateString = formatter.format(date);
        System.out.println(dateString);

        List<Table> availableTables = new ArrayList<>();

        for(Table table: tables){
            if(table.getBooked().containsKey(dateString)){
                System.out.println("found date");
                List<String> bookedTime = table.getBooked().get(dateString);
                System.out.println("booked times = " + bookedTime);
                if(!bookedTime.contains(timeSlotKey)){
                    System.out.println("not booked");
                    availableTables.add(table);
                }
            } else {
                availableTables.add(table);
            }
        }

        return availableTables;
    }

    public static List<String> getAvailableTimeSlot(List<String> bookedTimes, String date){
        List<String> allTimes = new ArrayList<>(List.of(
                "10:30-12:00",
                "12:15-13:45",
                "14:00-15:30",
                "15:45-17:15",
                "17:30-19:00",
                "19:15-20:45",
                "21:00-22:30"
        ));

        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate otherDate = LocalDate.parse(date, dateFormatter);

        if (today.isEqual(otherDate)) {
            LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata")).truncatedTo(ChronoUnit.MINUTES);

            allTimes.removeIf(timeSlot -> {
                String startTimeStr = timeSlot.split("-")[0];  // Get the start time
                LocalTime startTime = LocalTime.parse(startTimeStr);
                return startTime.isBefore(currentTime); // remove if start time is in the past
            });
        }

        if (bookedTimes != null) {
            allTimes.removeAll(bookedTimes);
        }

        return allTimes;
    }
}