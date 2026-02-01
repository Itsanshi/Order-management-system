package com.restaurantback.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesPerformanceDTO {
    private String locationId;
    private String reportFrom;
    private String reportTo;
    private int orderProcessedWithinLocation;
    private float deltaOfOrderProcessedToPreviousPeriod;
    private float averageCuisineFeedback;
    private float minimumCuisineFeedback;
    private float deltaOfAverageCuisineFeedbackToPreviousPeriod;
    private float revenueForOrders;
    private float deltaOfRevenueForOrdersToPreviousPeriod;
}
