package com.restaurantback.dto.reservation;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReservationDto {
    private String id;
    private String status;
    private String locationAddress;
    private String date;
    private String timeSlot;
    private String preOrder;
    private String guestsNumber;
    private String feedbackId;
    private String waiterName;
    private String waiterRating;
}
