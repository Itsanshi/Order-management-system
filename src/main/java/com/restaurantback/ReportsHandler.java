package com.restaurantback;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.report.FeedbackUpdate;
import com.restaurantback.models.*;
import com.restaurantback.repository.EmployeeRepository;
import com.restaurantback.repository.WaiterRepository;
import com.restaurantback.services.FeedbackService;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LambdaHandler(
        lambdaName = "reports_handler",
        roleName = "reports_handler-role",
        runtime = DeploymentRuntime.JAVA21,
        isPublishVersion = true,
        aliasName = "${lambdas_alias_name}",
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@DependsOn(name = "${report_sqs_queue}", resourceType = ResourceType.SQS_QUEUE)
@SqsTriggerEventSource(
        targetQueue = "${report_sqs_queue}",
        batchSize = 10
)

@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "waiterReportTable", value = "${waiter_report_table}"),
        @EnvironmentVariable(key = "locationReportTable", value = "${location_report_table}"),
        @EnvironmentVariable(key = "userTable", value = "${user_table}"),
        @EnvironmentVariable(key = "dishTable", value = "${dish_table}"),
        @EnvironmentVariable(key = "feedbackTable", value = "${feedback_table}"),
        @EnvironmentVariable(key = "locationTable", value = "${location_table}"),
        @EnvironmentVariable(key = "tablesTable", value = "${tables_table}"),
        @EnvironmentVariable(key = "timeslotTable", value = "${timeslot_table}"),
        @EnvironmentVariable(key = "reservationTable", value = "${reservation_table}"),
        @EnvironmentVariable(key = "bookingTable", value = "${booking_table}"),
        @EnvironmentVariable(key = "cartTable", value = "${cart_table}"),
        @EnvironmentVariable(key = "employeeTable", value = "${employee_table}"),
        @EnvironmentVariable(key = "waiterTable", value = "${waiter_table}")
})
public class ReportsHandler implements RequestHandler<SQSEvent, Map<String, Object>> {
    private Context context;

    private final SQSApplication sqsApplication = DaggerSQSApplication.create();

    private final ObjectMapper objectMapper = sqsApplication.getObjectMapper();
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient = sqsApplication.getDyaDynamoDbEnhancedClient();

    private final String waiterReportTableName = System.getenv("waiterReportTable");
    private final String locationReportTableName = System.getenv("locationReportTable");
    private final String bookingTableName = System.getenv("bookingTable");

    private final DynamoDbTable<WaiterReport> waiterReportDynamoDBTable = dynamoDbEnhancedClient.table(waiterReportTableName, TableSchema.fromBean(WaiterReport.class));
    private final DynamoDbTable<LocationReport> locationReportDynamoDbTable = dynamoDbEnhancedClient.table(locationReportTableName, TableSchema.fromBean(LocationReport.class));
    private final DynamoDbTable<Booking> bookingDynamoDbTable = dynamoDbEnhancedClient.table(bookingTableName, TableSchema.fromBean(Booking.class));

    private final EmployeeRepository employeeRepository = sqsApplication.getEmployeeRepository();
    private final WaiterRepository waiterRepository = sqsApplication.getWaiterRepository();
    private final FeedbackService feedbackService = sqsApplication.getFeedbackService();

    @Override
    public Map<String, Object> handleRequest(SQSEvent sqsEvent, Context context) {
        this.context = context;
        Map<String, Object> resultMap = new HashMap<>();

        try {
            for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {

                if (msg.getMessageAttributes() == null) {
                    throw new RuntimeException("Message Attributes are missing, one message attribute eventType must be there");
                }

                SQSEvent.MessageAttribute attribute = msg.getMessageAttributes().get("eventType");

                if (attribute == null) {
                    throw new RuntimeException("eventTypes is missing for the SQS Message");
                }

                String eventType = attribute.getStringValue();
                context.getLogger().log(eventType);

                switch (eventType) {
                    case "DAILY-EVENT":
                        dailyEventProcessing(msg.getBody());
                        break;
                    case "FEEDBACK_EVENT":
                        feedbackEventProcessing(msg.getBody());
                        break;
                    case "NEW_FEEDBACK_EVENT":
                        newFeedbackEventProcessing(msg.getBody());
                        break;
                    default:
                        context.getLogger().log(eventType + " is an unknown event type");
                }
            }

            resultMap.put("statusCode", 200);
            resultMap.put("message", "Reports have been uploaded");
        } catch (Exception e) {
            context.getLogger().log(e.toString());

            resultMap.put("statusCode", 500);
            resultMap.put("message", "Internal Server error, " + e.getMessage());
        }

        return resultMap;
    }

    private void newFeedbackEventProcessing(String body) {

        try {

            Feedback newFeedback = objectMapper.readValue(body, Feedback.class);

            if (newFeedback == null) {
                throw new RuntimeException("New feedback is null");
            }

            Booking booking = getBookingById(newFeedback.getId());
            System.out.println(booking);

            WaiterReport waiterReport = getWaiterReport(booking.getDate(), booking.getWaiterId());
            System.out.println(waiterReport);
            if (waiterReport == null) return;

            float avgFeedback = waiterReport.getAverageServiceFeedback();
            float minFeedback = waiterReport.getMinimumServiceFeedback() == 0 ? 10f: waiterReport.getMinimumServiceFeedback();
            int feedbackCount = waiterReport.getFeedbackCount();
            float totalFeedback = avgFeedback * feedbackCount;

            feedbackCount++;
            String serviceRating = newFeedback.getServiceRating();

            if (serviceRating != null && !serviceRating.isEmpty()) {
                try {
                    float serviceRatingFloat = Float.parseFloat(serviceRating);

                    minFeedback = Math.min(minFeedback, serviceRatingFloat);
                    avgFeedback = (totalFeedback + serviceRatingFloat) / feedbackCount;
                } catch (Exception e) {
                    throw new RuntimeException("Invalid service rating.", e);
                }
            }

            waiterReport.setAverageServiceFeedback(roundOffTo2DecPlaces(avgFeedback));
            waiterReport.setMinimumServiceFeedback(roundOffTo2DecPlaces(minFeedback));
            waiterReport.setFeedbackCount(feedbackCount);
            context.getLogger().log(waiterReport.toString());


            LocationReport locationReport = getLocationReport(booking.getDate(), booking.getLocationId());
            System.out.println(locationReport);
            if (locationReport == null) return;

            float avgCuisineFeedback = locationReport.getAverageCuisineFeedback();
            float minCuisineFeedback = locationReport.getMinimumCuisineFeedback() == 0 ? 10f : locationReport.getMinimumCuisineFeedback();
            int feedbackCountLocation = locationReport.getFeedbackCount();
            float totalFeedbackLocation = avgCuisineFeedback * feedbackCount;

            feedbackCountLocation++;
            String cuisineRating = newFeedback.getCuisineRating();

            if (cuisineRating != null && !cuisineRating.isEmpty()) {
                try {
                    float rating = Float.parseFloat(cuisineRating);
                    minCuisineFeedback = Math.min(minCuisineFeedback, rating);
                    avgCuisineFeedback = (totalFeedbackLocation + rating) / feedbackCountLocation;
                } catch (Exception e) {
                    throw new RuntimeException("Invalid cuisine rating", e);
                }
            }

            locationReport.setAverageCuisineFeedback(roundOffTo2DecPlaces(avgCuisineFeedback));
            locationReport.setMinimumCuisineFeedback(roundOffTo2DecPlaces(minCuisineFeedback));
            locationReport.setFeedbackCount(feedbackCountLocation);

            waiterReportDynamoDBTable.putItem(waiterReport);
            locationReportDynamoDbTable.putItem(locationReport);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error in processing new feedback in report handler.", e);
        }

    }

    private void feedbackEventProcessing(String body) {
        // Todo -> when feedback gets updated
        // by feedback id and res id get the reservation
        // get location report and waiter update the feedback accordingly
        try {
            FeedbackUpdate feedbackUpdate = objectMapper.readValue(body, FeedbackUpdate.class);

            if (feedbackUpdate == null) {
                throw new RuntimeException("Feedback update dto is null");
            }

            Feedback prevFeedback = feedbackUpdate.getPreviousFeedback();
            Feedback newFeedback = feedbackUpdate.getFeedback();
            System.out.println(prevFeedback);
            System.out.println(newFeedback);

            Booking booking = getBookingById(prevFeedback.getId());
            System.out.println(booking);

            WaiterReport waiterReport = getWaiterReport(booking.getDate(), booking.getWaiterId());
            System.out.println(waiterReport);
            if (waiterReport == null) return;

            LocationReport locationReport = getLocationReport(booking.getDate(), booking.getLocationId());
            System.out.println(locationReport);
            if (locationReport == null) return;

            float avgFeedback = waiterReport.getAverageServiceFeedback();
            float minFeedback = waiterReport.getMinimumServiceFeedback();
            int feedbackCount = waiterReport.getFeedbackCount();
            float totalFeedback = avgFeedback * feedbackCount;


            if (prevFeedback == null) {
                throw new RuntimeException("previous feedback is null");
            }

            if (newFeedback == null) {
                throw new RuntimeException("new feedback is null");
            }

            if (feedbackCount == 0) {
                throw new RuntimeException("feedback count is 0, error might be when feedback is created it is not updated");
            }

            System.out.println("waiter calc: " + avgFeedback + " " + minFeedback + " " + feedbackCount + " " + totalFeedback);
            String prevServiceRating = prevFeedback.getServiceRating();
            if (prevServiceRating != null && !prevServiceRating.isEmpty()) {
                try {
                    float prevServiceRatingFloat = Float.parseFloat(prevServiceRating);
                    totalFeedback -= prevServiceRatingFloat;

                    // If the minimum feedback was this one, we need to recalculate the minimum
                    if (Math.abs(minFeedback - prevServiceRatingFloat) < 0.001) {
                        minFeedback = Float.MAX_VALUE;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Invalid previous service rating.");
                }
            }
            System.out.println("waiter calc: " + avgFeedback + " " + minFeedback + " " + feedbackCount + " " + totalFeedback);

            String newServiceRating = newFeedback.getServiceRating();
            if (newServiceRating != null && !newServiceRating.isEmpty()) {
                try {
                    float newServiceRatingFloat = Float.parseFloat(newServiceRating);
                    totalFeedback += newServiceRatingFloat;
                    minFeedback = Math.min(minFeedback, newServiceRatingFloat);
                    avgFeedback = totalFeedback / feedbackCount;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Invalid new service rating.");
                }
            }

            System.out.println("waiter calc: " + avgFeedback + " " + minFeedback + " " + feedbackCount + " " + totalFeedback);

            waiterReport.setAverageServiceFeedback(roundOffTo2DecPlaces(avgFeedback));
            waiterReport.setMinimumServiceFeedback(roundOffTo2DecPlaces(minFeedback));

            float avgCuisineFeedback = locationReport.getAverageCuisineFeedback();
            float minCuisineFeedback = locationReport.getMinimumCuisineFeedback();
            int feedbackCountLocation = locationReport.getFeedbackCount();
            float totalCuisineFeedback = avgCuisineFeedback * feedbackCountLocation;
            System.out.println("location calc: " + avgFeedback + " " + minFeedback + " " + feedbackCount + " " + totalFeedback);

            String prevCuisineRating = prevFeedback.getCuisineRating();
            if (prevCuisineRating != null && !prevCuisineRating.isEmpty()) {
                try {
                    float prevCuisineRatingFloat = Float.parseFloat(prevCuisineRating);
                    totalCuisineFeedback -= prevCuisineRatingFloat;

                    // If the minimum feedback was this one, we need to recalculate the minimum
                    if (Math.abs(minCuisineFeedback - prevCuisineRatingFloat) < 0.001) {
                        minCuisineFeedback = Float.MAX_VALUE;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Invalid previous cuisine rating.");
                }
            }
            System.out.println("location calc: " + avgFeedback + " " + minFeedback + " " + feedbackCount + " " + totalFeedback);

            // Now add the new feedback
            String newCuisineRating = newFeedback.getCuisineRating();
            if (newCuisineRating != null && !newCuisineRating.isEmpty()) {
                try {
                    float newCuisineRatingFloat = Float.parseFloat(newCuisineRating);
                    totalCuisineFeedback += newCuisineRatingFloat;
                    minCuisineFeedback = Math.min(minCuisineFeedback, newCuisineRatingFloat);
                    avgCuisineFeedback = totalCuisineFeedback / feedbackCountLocation;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Invalid new cuisine rating.");
                }
            }

            System.out.println("location calc: " + avgFeedback + " " + minFeedback + " " + feedbackCount + " " + totalFeedback);

            locationReport.setAverageCuisineFeedback(roundOffTo2DecPlaces(avgCuisineFeedback));
            locationReport.setMinimumCuisineFeedback(roundOffTo2DecPlaces(minCuisineFeedback));

            System.out.println(waiterReport);
            System.out.println(locationReport);

            waiterReportDynamoDBTable.putItem(waiterReport);
            locationReportDynamoDbTable.putItem(locationReport);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Booking getBookingById(String feedbackId) {
        Expression filterExpression = Expression.builder()
                .expression("feedbackId = :feedbackIdValue")
                .putExpressionValue(":feedbackIdValue", AttributeValue.builder().s(feedbackId).build())
                .build();

        // Create a scan request with the filter
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        // Execute the scan and use streams to find the first match
        PageIterable<Booking> results = bookingDynamoDbTable.scan(scanRequest);

        return results.stream()
                .flatMap(page -> page.items().stream())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Booking with this feedback id doesn't exits"));
    }

    private void dailyEventProcessing(String body) {
        try {
            context.getLogger().log("Processing daily event...");

            Booking booking = objectMapper.readValue(body, Booking.class);
            context.getLogger().log("Booking: " + booking.toString());

            String waiterId = booking.getWaiterId();
            String locationId = booking.getLocationId();
            String feedbackId = booking.getFeedbackId();
            context.getLogger().log("waiterId: " + waiterId);
            context.getLogger().log("locationId: " + locationId);
            context.getLogger().log("feedbackId: " + feedbackId);

            context.getLogger().log("Fetching feedback ...");
            Feedback feedback = null;
            if (!feedbackId.equalsIgnoreCase("no_feedback")) {
                feedback = feedbackService.getFeedbackById(feedbackId);
                context.getLogger().log("Feedback :" + feedback.toString());
            }

            processReportForWaiter(booking, feedback, waiterId);
            processReportForLocation(booking, feedback, locationId);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private void processReportForLocation(Booking booking, Feedback feedback, String locationId) {
        context.getLogger().log("Processing Location and their reports...");

        LocationReport locationReport = getLocationReport(booking.getDate(), locationId);
        context.getLogger().log("Location Report: " + locationReport);

        if (locationReport == null) {
            locationReport = processIfLocationReportIsNull(booking, feedback, locationId);
        } else {
            processIfLocationReportIsPresent(booking, feedback, locationReport);
        }

        locationReportDynamoDbTable.putItem(locationReport);
        context.getLogger().log("location report updated: " + locationReport);
    }

    private LocationReport processIfLocationReportIsNull(Booking booking, Feedback feedback, String locationId) {
        context.getLogger().log("Location report does not exists for this Location so creating one...");
        LocationReport locationReport = new LocationReport();

        locationReport.setLocationId(locationId);
        locationReport.setDate(booking.getDate());
        locationReport.setOrderProcessed(1);

        float avgCuisineFeedback = 0.0f;
        float minCuisineFeedback = 0.0f;
        int feedbackCount = 0;

        if (feedback != null) {
            feedbackCount++;
            String cuisineRating = feedback.getCuisineRating();

            if (cuisineRating != null && !cuisineRating.isEmpty()) {
                try {
                    minCuisineFeedback = Float.parseFloat(cuisineRating);
                    avgCuisineFeedback = minCuisineFeedback;
                } catch (Exception e) {
                    throw new RuntimeException("Invalid cuisine rating");
                }
            }
        }

        locationReport.setAverageCuisineFeedback(roundOffTo2DecPlaces(avgCuisineFeedback));
        locationReport.setMinimumCuisineFeedback(roundOffTo2DecPlaces(minCuisineFeedback));
        locationReport.setFeedbackCount(feedbackCount);

        // Todo -> process revenue
        locationReport.setRevenue(1);
        return locationReport;
    }

    private void processIfLocationReportIsPresent(Booking booking, Feedback feedback, LocationReport locationReport) {
        context.getLogger().log("processing location report if report is already present...");

        locationReport.setOrderProcessed(locationReport.getOrderProcessed() + 1);

        float avgCuisineFeedback = locationReport.getAverageCuisineFeedback();
        float minCuisineFeedback = locationReport.getMinimumCuisineFeedback();
        int feedbackCount = locationReport.getFeedbackCount();

        float totalFeedback = avgCuisineFeedback * feedbackCount;

        if (feedback != null) {
            feedbackCount++;
            String cuisineRating = feedback.getCuisineRating();

            if (cuisineRating != null && !cuisineRating.isEmpty()) {
                try {
                    float rating = Float.parseFloat(cuisineRating);
                    minCuisineFeedback = Math.min(minCuisineFeedback, rating);
                    avgCuisineFeedback = (totalFeedback + rating) / feedbackCount;
                } catch (Exception e) {
                    throw new RuntimeException("Invalid cuisine rating");
                }
            }
        }

        locationReport.setAverageCuisineFeedback(roundOffTo2DecPlaces(avgCuisineFeedback));
        locationReport.setMinimumCuisineFeedback(roundOffTo2DecPlaces(minCuisineFeedback));
        locationReport.setFeedbackCount(feedbackCount);

        // Todo -> process revenue
        locationReport.setRevenue(locationReport.getRevenue() + 1);
    }

    private void processReportForWaiter(Booking booking, Feedback feedback, String waiterId) {
        context.getLogger().log("Processing Waiters and their reports...");

        WaiterReport waiterReport = getWaiterReport(booking.getDate(), waiterId);
        context.getLogger().log("Waiter report: " + waiterReport);

        context.getLogger().log("Fetching employee, if error might be in gsi");
        Employee employee = employeeRepository.getEmployeeWithId(waiterId);
        context.getLogger().log("Employee :" + employee.toString());

        if (waiterReport == null) {
            waiterReport = processIfWaiterReportNull(employee, booking, feedback);
        } else {
            processIfWaiterReportPresent(waiterReport, booking, feedback);
        }

        waiterReportDynamoDBTable.putItem(waiterReport);
        context.getLogger().log("waiter report updated: " + waiterReport);
    }

    private WaiterReport getWaiterReport(String date, String id) {
        Key key = Key.builder()
                .partitionValue(date)
                .sortValue(id)
                .build();

        return waiterReportDynamoDBTable.getItem(item -> item.key(key));
    }

    private LocationReport getLocationReport(String date, String locationId) {
        Key key = Key.builder()
                .partitionValue(date)
                .sortValue(locationId)
                .build();

        return locationReportDynamoDbTable.getItem(item -> item.key(key));
    }

    private float calculateHours(String timeFrom, String timeTo) {
        LocalTime from = LocalTime.parse(timeFrom);
        LocalTime to = LocalTime.parse(timeTo);

        Duration duration = Duration.between(from, to);
        context.getLogger().log(String.valueOf(duration.toHours()));

        return duration.toMinutes() / 60.0f;
    }

    private String getTimeSlot(String timeFrom, String timeTo) {
        return timeFrom + "-" + timeTo;
    }

    private WaiterReport processIfWaiterReportNull(
            Employee employee,
            Booking booking,
            Feedback feedback
    ) {
        context.getLogger().log("Waiter report does not exists for this waiter so creating one...");
        WaiterReport waiterReport = new WaiterReport();
        String waiterId = booking.getWaiterId();
        String locationId = booking.getLocationId();

        // set waiter's details
        waiterReport.setDate(booking.getDate());
        waiterReport.setEmail(employee.getEmail());
        waiterReport.setName(employee.getFirstName() + " " + employee.getLastName());
        waiterReport.setLocationId(locationId);
        waiterReport.setWaiterId(waiterId);

        // calculate working hours
        int timeSlotIndex = waiterRepository.getTimeSlotIndex(waiterId, locationId, booking.getDate(), getTimeSlot(booking.getTimeFrom(), booking.getTimeTo()));
        context.getLogger().log("time slot index: " + timeSlotIndex);
        if (timeSlotIndex == -1) {
            throw new RuntimeException("This reservation is not assigned to waiter");
        }

        waiterRepository.removeTimeSlotFromWaiter(waiterId, locationId, booking.getDate(), getTimeSlot(booking.getTimeFrom(), booking.getTimeTo()));
        context.getLogger().log("removed time slot from waiter");

        float workingHours = calculateHours(booking.getTimeFrom(), booking.getTimeTo());
        waiterReport.setWorkingHours(workingHours);

        waiterReport.setOrderProcessed(1);

        float avgFeedback = 0.0f;
        float minFeedback = 0.0f;
        int feedbackCount = 0;

        if (feedback != null) {
            feedbackCount++;
            String serviceRating = feedback.getServiceRating();

            if (serviceRating != null && !serviceRating.isEmpty()) {
                try {
                    minFeedback = Float.parseFloat(serviceRating);
                    avgFeedback = minFeedback;
                } catch (Exception e) {
                    throw new RuntimeException("Invalid service rating");
                }
            }
        }

        waiterReport.setAverageServiceFeedback(roundOffTo2DecPlaces(avgFeedback));
        waiterReport.setMinimumServiceFeedback(roundOffTo2DecPlaces(minFeedback));
        waiterReport.setFeedbackCount(feedbackCount);

        return waiterReport;
    }

    private void processIfWaiterReportPresent(
            WaiterReport waiterReport,
            Booking booking,
            Feedback feedback
    ) {
        context.getLogger().log("processing waiter report if report is already present...");
        String waiterId = booking.getWaiterId();
        String locationId = booking.getLocationId();

        int timeSlotIndex = waiterRepository.getTimeSlotIndex(waiterId, locationId, booking.getDate(), getTimeSlot(booking.getTimeFrom(), booking.getTimeTo()));
        context.getLogger().log("time slot index: " + timeSlotIndex);

        if (timeSlotIndex != -1) {
            waiterRepository.removeTimeSlotFromWaiter(waiterId, locationId, booking.getDate(), getTimeSlot(booking.getTimeFrom(), booking.getTimeTo()));
            context.getLogger().log("removing timeslot from waiter");

            float workingHours = waiterReport.getWorkingHours() + calculateHours(booking.getTimeFrom(), booking.getTimeTo());
            waiterReport.setWorkingHours(workingHours);
        }

        waiterReport.setOrderProcessed(waiterReport.getOrderProcessed() + 1);

        float avgFeedback = waiterReport.getAverageServiceFeedback();
        float minFeedback = waiterReport.getMinimumServiceFeedback();
        int feedbackCount = waiterReport.getFeedbackCount();
        float totalFeedback = avgFeedback * feedbackCount;

        if (feedback != null) {
            feedbackCount++;
            String serviceRating = feedback.getServiceRating();

            if (serviceRating != null && !serviceRating.isEmpty()) {
                try {
                    float serviceRatingFloat = Float.parseFloat(serviceRating);
                    minFeedback = Math.min(minFeedback, serviceRatingFloat);
                    avgFeedback = (totalFeedback + serviceRatingFloat) / feedbackCount;
                } catch (Exception e) {
                    throw new RuntimeException("Invalid service rating.");
                }
            }
        }

        waiterReport.setAverageServiceFeedback(roundOffTo2DecPlaces(avgFeedback));
        waiterReport.setMinimumServiceFeedback(roundOffTo2DecPlaces(minFeedback));
        waiterReport.setFeedbackCount(feedbackCount);

        context.getLogger().log(waiterReport.toString());
    }

    private float roundOffTo2DecPlaces(float val) {
        String res = String.format("%.2f", val);
        return Float.parseFloat(res);
    }
}
