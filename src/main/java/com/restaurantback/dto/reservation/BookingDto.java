package com.restaurantback.dto.reservation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BookingDto {
    private String reservation_id;
    private String locationId;      // Add this
    private String tableId;
    private String date;
    private String waiterId;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;
    private String feedbackId;
    private Boolean byCustomer;
    @JsonIgnore private String userEmail;       // This is set internally


}
