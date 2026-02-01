package com.restaurantback.dto;

import lombok.Data;

@Data
public class ApiFeedbackModificationDTO {
    private String feedbackId;
    private String cuisineComment;
    private String serviceComment;
    private String serviceRating;
    private String cuisineRating;

}
