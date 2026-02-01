package com.restaurantback.services;

import com.restaurantback.dto.report.SalesPerformanceDTO;
import com.restaurantback.models.LocationReport;
import com.restaurantback.utils.RoundOffDecPlaces;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class SalesReportsService {
    private final DynamoDbTable<LocationReport> locationReportDynamoDBTable;

    public SalesReportsService(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        String locationReportTableName = System.getenv("locationReportTable");
        locationReportDynamoDBTable = dynamoDbEnhancedClient.table(locationReportTableName, TableSchema.fromBean(LocationReport.class));
    }

    public List<SalesPerformanceDTO> generateSalesPerformanceReport(String startDate, String endDate) {

        System.out.println("Fetch all location reports for the current period");
        List<LocationReport> currentPeriodReports = getLocationReportsForDateRangeUsingScan(startDate, endDate);

        System.out.println("Calculate the previous period dates (same length as current period)");
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        int periodLength = (int) ChronoUnit.DAYS.between(start, end) + 1;

        LocalDate prevStart = start.minusDays(periodLength);
        LocalDate prevEnd = end.minusDays(periodLength);
        String prevStartDate = prevStart.toString();
        String prevEndDate = prevEnd.toString();

        System.out.println("Fetch all location reports for the previous period");
        List<LocationReport> previousPeriodReports = getLocationReportsForDateRangeUsingScan(prevStartDate, prevEndDate);

        System.out.println("Group reports by location ID for both periods");
        Map<String, List<LocationReport>> currentReportsByLocation = currentPeriodReports.stream()
                .collect(Collectors.groupingBy(LocationReport::getLocationId));

        Map<String, List<LocationReport>> previousReportsByLocation = previousPeriodReports.stream()
                .collect(Collectors.groupingBy(LocationReport::getLocationId));

        System.out.println("Process each location's data");
        List<SalesPerformanceDTO> performanceReports = new ArrayList<>();

        for (String locationId : currentReportsByLocation.keySet()) {
            List<LocationReport> locationCurrentReports = currentReportsByLocation.get(locationId);

            // Skip if no reports for this location
            if (locationCurrentReports.isEmpty()) {
                continue;
            }

            System.out.println("Calculate aggregated metrics for current period");
            int totalOrdersProcessed = calculateTotalOrdersProcessed(locationCurrentReports);
            float avgCuisineFeedback = calculateAverageCuisineFeedback(locationCurrentReports);
            float minCuisineFeedback = calculateMinimumCuisineFeedback(locationCurrentReports);
            float totalRevenue = calculateTotalRevenue(locationCurrentReports);

            System.out.println("Calculate metrics for previous period (if available)");
            int prevTotalOrdersProcessed = 0;
            float prevAvgCuisineFeedback = 0;
            float prevTotalRevenue = 0;

            List<LocationReport> locationPreviousReports = previousReportsByLocation.getOrDefault(locationId, Collections.emptyList());
            if (!locationPreviousReports.isEmpty()) {
                prevTotalOrdersProcessed = calculateTotalOrdersProcessed(locationPreviousReports);
                prevAvgCuisineFeedback = calculateAverageCuisineFeedback(locationPreviousReports);
                prevTotalRevenue = calculateTotalRevenue(locationPreviousReports);
            }

            System.out.println("Calculate delta metrics");
            float ordersDelta = calculateDeltaPercentage(totalOrdersProcessed, prevTotalOrdersProcessed);
            float feedbackDelta = calculateDeltaPercentage(avgCuisineFeedback, prevAvgCuisineFeedback);
            float revenueDelta = calculateDeltaPercentage(totalRevenue, prevTotalRevenue);

            // Create the DTO
            SalesPerformanceDTO performanceDTO = new SalesPerformanceDTO(
                    locationId,
                    startDate,
                    endDate,
                    totalOrdersProcessed,
                    RoundOffDecPlaces.roundOffTo2DecPlaces(ordersDelta),
                    RoundOffDecPlaces.roundOffTo2DecPlaces(avgCuisineFeedback),
                    RoundOffDecPlaces.roundOffTo2DecPlaces(minCuisineFeedback),
                    RoundOffDecPlaces.roundOffTo2DecPlaces(feedbackDelta),
                    RoundOffDecPlaces.roundOffTo2DecPlaces(totalRevenue),
                    RoundOffDecPlaces.roundOffTo2DecPlaces(revenueDelta)
            );

            System.out.println(performanceDTO);

            performanceReports.add(performanceDTO);
        }
        System.out.println(performanceReports);
        return performanceReports;
    }

    public List<LocationReport> getLocationReportsForDateRangeUsingScan(String startDate, String endDate) {
        System.out.println("Scanning for location reports from " + startDate + " to " + endDate);

        // Create a filter expression for the date range
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":startDate", AttributeValue.builder().s(startDate).build());
        expressionValues.put(":endDate", AttributeValue.builder().s(endDate).build());

        Expression filterExpression = Expression.builder()
                .expression("local_date BETWEEN :startDate AND :endDate")
                .expressionValues(expressionValues)
                .build();

        // Scan the table with the filter
        return locationReportDynamoDBTable.scan(r -> r.filterExpression(filterExpression))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    /**
     * Get location reports for a specific date
     */
    private List<LocationReport> getLocationReportsForDate(String date) {
        Key key = Key.builder()
                .partitionValue(date)
                .build();

        QueryConditional queryConditional = QueryConditional.keyEqualTo(key);

        return locationReportDynamoDBTable.query(r -> r.queryConditional(queryConditional))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public LocationReport getLocationReportForDateAndLocation(String date, String locationId) {
        Key key = Key.builder()
                .partitionValue(date)
                .sortValue(locationId)
                .build();

        return locationReportDynamoDBTable.getItem(key);
    }


    private List<String> generateDateRange(String startDate, String endDate) {
        List<String> dates = new ArrayList<>();

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current.toString());
            current = current.plusDays(1);
        }

        return dates;
    }

    public List<LocationReport> getLocationReportsForDateRange(String startDate, String endDate) {
        List<String> dateRange = generateDateRange(startDate, endDate);

        return dateRange.parallelStream()
                .map(this::getLocationReportsForDate)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /* up to here for some optimization*/

    private int calculateTotalOrdersProcessed(List<LocationReport> reports) {
        return reports.stream()
                .mapToInt(LocationReport::getOrderProcessed)
                .sum();
    }

    private float calculateAverageCuisineFeedback(List<LocationReport> reports) {
        // Calculate weighted average based on feedback count
        int totalFeedbackCount = reports.stream()
                .mapToInt(LocationReport::getFeedbackCount)
                .sum();

        if (totalFeedbackCount == 0) {
            return 0;
        }

        float weightedSum = 0;
        for (LocationReport report : reports) {
            if (report.getFeedbackCount() > 0) {
                weightedSum += report.getAverageCuisineFeedback() * report.getFeedbackCount();
            }
        }

        return weightedSum / totalFeedbackCount;
    }

    private float calculateMinimumCuisineFeedback(List<LocationReport> reports) {
        return (float) reports.stream()
                .filter(r -> r.getFeedbackCount() > 0)
                .mapToDouble(LocationReport::getMinimumCuisineFeedback)
                .min()
                .orElse(0);
    }

    private float calculateTotalRevenue(List<LocationReport> reports) {
        return (float) reports.stream()
                .mapToDouble(LocationReport::getRevenue)
                .sum();
    }

    private float calculateDeltaPercentage(float current, float previous) {
        if (previous == 0) {
            return (current > 0) ? 100 : 0; // 100% increase if previous was 0
        }

        return ((current - previous) / previous) * 100;
    }

}
