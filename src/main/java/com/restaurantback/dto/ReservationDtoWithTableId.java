package com.restaurantback.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReservationDtoWithTableId {
    private String id;
    private String status;
    private String locationAddress;
    private String date;
    private String timeSlot;
    private String preOrder;
    private String guestsNumber;
    private String feedbackId;
    private String tableId;
    private String waiterName;
    private String waiterRating;
}
