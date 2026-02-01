package com.restaurantback.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WaiterBookingDTO {

    private String clientType;
    private String clientEmail;
    private String date;
    private String guestsNumber;
    private String tableNumber;
    private String timeFrom;
    private String timeTo;
}
