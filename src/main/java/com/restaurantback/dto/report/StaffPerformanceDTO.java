package com.restaurantback.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffPerformanceDTO {
    private String locationId;
    private String staffName;
    private String staffEmail;
    private String reportFrom;
    private String reportTo;
    private float workingHours;
    private int orderProcessed;
    private float deltaOfOrderProcessedToPreviousPeriod;
    private float averageServiceFeedback;
    private float minimumServiceFeedback;
    private float deltaOfAverageServiceFeedbackToPreviousPeriod;
}
