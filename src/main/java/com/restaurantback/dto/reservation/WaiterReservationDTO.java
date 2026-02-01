package com.restaurantback.dto.reservation;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class WaiterReservationDTO {
    private String locationName;
    private String tableNumber;
    private String date;
    private String time;
    private String bookingDoneBy;
    private String guests;
    private String reservationId;
    private String status;
    private String waiter_id;
}