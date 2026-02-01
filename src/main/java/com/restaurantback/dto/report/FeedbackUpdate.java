package com.restaurantback.dto.report;

import com.restaurantback.models.Feedback;
import lombok.Data;

@Data
public class FeedbackUpdate {
    private Feedback feedback;
    private Feedback previousFeedback;
}
