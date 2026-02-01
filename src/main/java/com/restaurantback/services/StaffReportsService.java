package com.restaurantback.services;

import com.restaurantback.dto.report.SalesPerformanceDTO;
import com.restaurantback.dto.report.StaffPerformanceDTO;
import com.restaurantback.models.WaiterReport;
import com.restaurantback.utils.RoundOffDecPlaces;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StaffReportsService {

    private final DynamoDbTable<WaiterReport> waiterReportDynamoDBTable;

    public StaffReportsService(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        String waiterReportTableName = System.getenv("waiterReportTable");
        waiterReportDynamoDBTable = dynamoDbEnhancedClient.table(waiterReportTableName, TableSchema.fromBean(WaiterReport.class));
    }

    public List<StaffPerformanceDTO> generateStaffPerformanceReport(String reportFrom, String reportTo) {
        List<WaiterReport> currentPeriodReports = getWaiterReportsForDateRangeUsingScan(reportFrom, reportTo);

        System.out.println("Staff performance processing...");
        LocalDate start = LocalDate.parse(reportFrom);
        LocalDate end = LocalDate.parse(reportTo);
        int periodLength = (int) ChronoUnit.DAYS.between(start, end) + 1;

        LocalDate prevStart = start.minusDays(periodLength);
        LocalDate prevEnd = end.minusDays(periodLength);
        String prevStartDate = prevStart.toString();
        String prevEndDate = prevEnd.toString();

        List<WaiterReport> previousPeriodReports = getWaiterReportsForDateRangeUsingScan(prevStartDate, prevEndDate);
        
        Map<String, List<WaiterReport>> currentReportsByWaiter = currentPeriodReports.stream()
                .collect(Collectors.groupingBy(WaiterReport::getWaiterId));

        Map<String, List<WaiterReport>> previousReportsByWaiter = previousPeriodReports.stream()
                .collect(Collectors.groupingBy(WaiterReport::getWaiterId));


        System.out.println("Process each waiter's data");
        List<StaffPerformanceDTO> performanceReports = new ArrayList<>();

        for (String waiterId : currentReportsByWaiter.keySet()) {
            List<WaiterReport> waiterCurrentReports = currentReportsByWaiter.get(waiterId);

            System.out.println("Skip if no reports for this waiter");
            if (waiterCurrentReports.isEmpty()) {
                continue;
            }

            System.out.println("Get waiter details from the first report");
            WaiterReport firstReport = waiterCurrentReports.getFirst();
            String waiterName = firstReport.getName();
            String waiterEmail = firstReport.getEmail();
            String locationId = firstReport.getLocationId();

            System.out.println("Calculate aggregated metrics for current period");
            float totalWorkingHours = calculateTotalWorkingHours(waiterCurrentReports);
            int totalOrdersProcessed = calculateTotalOrdersProcessed(waiterCurrentReports);
            float avgServiceFeedback = calculateAverageServiceFeedback(waiterCurrentReports);
            float minServiceFeedback = calculateMinimumServiceFeedback(waiterCurrentReports);

            System.out.println("Calculate metrics for previous period (if available)");
            float prevTotalOrdersProcessed = 0;
            float prevAvgServiceFeedback = 0;

            List<WaiterReport> waiterPreviousReports = previousReportsByWaiter.getOrDefault(waiterId, Collections.emptyList());
            if (!waiterPreviousReports.isEmpty()) {
                prevTotalOrdersProcessed = calculateTotalOrdersProcessed(waiterPreviousReports);
                prevAvgServiceFeedback = calculateAverageServiceFeedback(waiterPreviousReports);
            }

            System.out.println("Calculate delta metrics");
            float ordersDelta = calculateDeltaPercentage(totalOrdersProcessed, prevTotalOrdersProcessed);
            float feedbackDelta = calculateDeltaPercentage(avgServiceFeedback, prevAvgServiceFeedback);

            // Create the DTO
            StaffPerformanceDTO performanceDTO = new StaffPerformanceDTO(
                    locationId,
                    waiterName,
                    waiterEmail,
                    reportFrom,
                    reportTo,
                    RoundOffDecPlaces.roundOffTo2DecPlaces(totalWorkingHours),
                    totalOrdersProcessed,
                    RoundOffDecPlaces.roundOffTo2DecPlaces(ordersDelta),
                    RoundOffDecPlaces.roundOffTo2DecPlaces(avgServiceFeedback),
                    RoundOffDecPlaces.roundOffTo2DecPlaces(minServiceFeedback),
                    RoundOffDecPlaces.roundOffTo2DecPlaces(feedbackDelta)
            );
            System.out.println(performanceDTO);
            performanceReports.add(performanceDTO);
        }

        System.out.println(performanceReports);
        return performanceReports;
    }

    private float calculateTotalWorkingHours(List<WaiterReport> reports) {
        return (float) reports.stream()
                .mapToDouble(WaiterReport::getWorkingHours)
                .sum();
    }

    private int calculateTotalOrdersProcessed(List<WaiterReport> reports) {
        return reports.stream()
                .mapToInt(WaiterReport::getOrderProcessed)
                .sum();
    }

    private float calculateAverageServiceFeedback(List<WaiterReport> reports) {
        int totalFeedbackCount = reports.stream()
                .mapToInt(WaiterReport::getFeedbackCount)
                .sum();

        if (totalFeedbackCount == 0) {
            return 0;
        }

        float weightedSum = 0;
        for (WaiterReport report : reports) {
            if (report.getFeedbackCount() > 0) {
                weightedSum += report.getAverageServiceFeedback() * report.getFeedbackCount();
            }
        }

        return weightedSum / totalFeedbackCount;
    }

    private float calculateMinimumServiceFeedback(List<WaiterReport> reports) {
        return (float) reports.stream()
                .filter(r -> r.getFeedbackCount() > 0)
                .mapToDouble(WaiterReport::getMinimumServiceFeedback)
                .min()
                .orElse(0);
    }

    private float calculateDeltaPercentage(float current, float previous) {
        if (previous == 0) {
            return (current > 0) ? 100 : 0; // 100% increase if previous was 0
        }

        return ((current - previous) / previous) * 100;
    }

    public List<WaiterReport> getWaiterReportsForDateRangeUsingScan(String startDate, String endDate) {
        System.out.println("Scanning for waiter reports from " + startDate + " to " + endDate);

        Expression filterExpression = Expression.builder()
                .expression("local_date BETWEEN :startDate AND :endDate")
                .expressionValues(Map.of(
                        ":startDate", AttributeValue.builder().s(startDate).build(),
                        ":endDate", AttributeValue.builder().s(endDate).build()))
                .build();

        return waiterReportDynamoDBTable.scan(r -> r.filterExpression(filterExpression))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

}
