package com.restaurantback.services;

import com.restaurantback.dto.ReservationByWaiterDTO;
import com.restaurantback.dto.WaiterBookingDTO;
import com.restaurantback.exceptions.reservationException.ConflictException;
import com.restaurantback.exceptions.reservationException.ForbiddenException;
import com.restaurantback.exceptions.reservationException.NotFoundException;
import com.restaurantback.models.Booking;
import com.restaurantback.models.Table;
import com.restaurantback.models.Waiter;
import com.restaurantback.repository.*;

import java.util.List;
import java.util.Map;

public class ReservationService {
    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final WaiterRepository waiterRepository;
    private final TableRepository tableRepository;
    private final LocationService locationService;
    private final UserRepository userRepository;

    public ReservationService(BookingRepository bookingRepository, EmployeeRepository employeeRepository, WaiterRepository waiterRepository, TableRepository tableRepository, LocationService locationService, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.employeeRepository = employeeRepository;
        this.waiterRepository = waiterRepository;
        this.tableRepository = tableRepository;
        this.locationService = locationService;
        this.userRepository = userRepository;
    }

    public ReservationByWaiterDTO bookTable(String email, WaiterBookingDTO waiterBookingDTO, String id) {
        String date = waiterBookingDTO.getDate();
        String from = waiterBookingDTO.getTimeFrom();
        String to = waiterBookingDTO.getTimeTo();
        Booking booking = new Booking();
        boolean flag = false;

        if (Integer.parseInt(waiterBookingDTO.getGuestsNumber()) < 1 && Integer.parseInt(waiterBookingDTO.getGuestsNumber()) > 10) {
            throw new ForbiddenException("Guests numbers should be between 1 and 10");
        }

        String waiterId = employeeRepository.getWaiterIdFromEmail(email);
        String locationId = employeeRepository.getLocationIdFromEmail(email);
        if (waiterAvailableOnDateAndTime(waiterId, date, from, to) && tableAvailableOnDateAndTime(waiterBookingDTO.getTableNumber(), date, from, to, locationId)) {
            if (waiterBookingDTO.getClientType().equalsIgnoreCase("visitor")) {
                booking = bookingRepository.makeBookingForVisitor(waiterBookingDTO, waiterId, id, locationId);
            } else if (waiterBookingDTO.getClientType().equalsIgnoreCase("customer")) {
                booking = bookingRepository.makeBookingForCustomer(waiterBookingDTO, waiterId, id, locationId);
                flag = true;
            } else {
                throw new ForbiddenException("Not allowed");
            }
        }

        waiterRepository.updateTimeSlotForWaiterWithId(waiterId, booking.getLocationId(), booking.getDate(), booking.getTimeFrom() + "-" + booking.getTimeTo());
        tableRepository.updateTimeSlotForTableWithId(booking.getTableId(), booking.getLocationId(), booking.getDate(), booking.getTimeFrom() + "-" + booking.getTimeTo());

        return convertToDTO(booking, flag, email);
    }

    private ReservationByWaiterDTO convertToDTO(Booking booking, Boolean flag, String waiterEmail) {
        ReservationByWaiterDTO dto = new ReservationByWaiterDTO();
        dto.setDate(booking.getDate());
        dto.setGuestsNumber(booking.getGuestsNumber());
        dto.setId(booking.getReservationId());
        dto.setLocationAddress(locationService.getLocationAddressById(booking.getLocationId()));
        dto.setStatus(booking.getStatus());
        dto.setTableNumber(booking.getTableId());
        dto.setTimeSlot(booking.getTimeFrom() + "-" + booking.getTimeTo());
        if (flag) {
            dto.setUserInfo("Customer " + userRepository.getName(booking.getUserEmail()));
        } else {
            dto.setUserInfo("Waiter " + employeeRepository.getName(waiterEmail));
        }

        return dto;
    }

    private boolean tableAvailableOnDateAndTime(String tableNumber, String date, String from, String to, String locationId) {
        Table table = tableRepository.getTableByIdAndLocationId(tableNumber, locationId);
        System.out.println(tableNumber);
        System.out.println(table);
        if (table == null) {
            throw new NotFoundException("Table not found");
        }

        Map<String, List<String>> booked = table.getBooked();
        System.out.println(booked);

        if (booked != null && booked.containsKey(date) && booked.get(date).contains(from + "-" + to)) {
            throw new ConflictException("Reservation Already at that time and date");
        }


        return true;

    }

    private boolean waiterAvailableOnDateAndTime(String waiterId, String date, String from, String to) {
        Waiter waiter = waiterRepository.getWaiterById(waiterId);
        if (waiter == null) {
            throw new NotFoundException("Waiter Not found");
        }

        Map<String, List<String>> booked = waiter.getBooked();

        if (booked != null && booked.containsKey(date) && booked.get(date).contains(from + "-" + to)) {
            throw new ConflictException("Reservation at that date and time already exixts");
        }


        return true;
    }
}
