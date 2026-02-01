package com.restaurantback.dto;
import lombok.Data;

@Data
public class ApiFeedbackDTO {
    private String cuisineComment;
    private String serviceComment;
    private String serviceRating;
    private String cuisineRating;
    private String reservationId;
}

