package com.restaurantback.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationByWaiterDTO {
    private String date;
    private String feedbackId;
    private String guestsNumber;
    private String id;
    private String locationAddress;
    private String preOrder;
    private String status;
    private String tableNumber;
    private String timeSlot;
    private String userInfo;
}
