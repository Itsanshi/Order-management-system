package com.restaurantback;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.restaurantback.models.Booking;
import com.restaurantback.repository.BookingRepository;
import com.restaurantback.services.BookingService;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import org.json.JSONObject;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STIconSetType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LambdaHandler(
        lambdaName = "sqs_event_sender_handler",
        roleName = "sqs_event_sender_handler-role",
        isPublishVersion = true,
        aliasName = "${lambdas_alias_name}",
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
        runtime = DeploymentRuntime.JAVA21
)

@DependsOn(name = "${report_sqs_queue}", resourceType = ResourceType.SQS_QUEUE)
@RuleEventSource(targetRule = "${update_event_rule}")

@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "reportQueue", value = "${report_sqs_queue}"),
        @EnvironmentVariable(key = "bookingTable", value = "${booking_table}")
})

public class SqsEventSenderHandler implements RequestHandler<ScheduledEvent, Map<String, Object>> {

    private final String queueName = System.getenv("reportQueue");

    private final SQSApplication sqsApplication = DaggerSQSApplication.create();

    private final AmazonSQS amazonSQSClient = sqsApplication.getAmazonSqsClient();
    private final BookingService bookingService = sqsApplication.getBookingService();
    private final BookingRepository bookingRepository = sqsApplication.getBookingRepository();

    @Override
    public Map<String, Object> handleRequest(ScheduledEvent request, Context context) {

        context.getLogger().log("Cron Event has hit and Handler is launched...");
        Map<String, Object> resultMap = new HashMap<String, Object>();

        try {
            context.getLogger().log("Request for update all the reservations...");
            bookingService.updateBookings();
            context.getLogger().log("Reservations are updated");

            context.getLogger().log("Get all reservations for today...");
            LocalDate currDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalTime currTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
            LocalTime prevTime = currTime.minusMinutes(120);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String formattedCurrTime = currTime.format(formatter);
            String formattedPrevTime = prevTime.format(formatter);
            context.getLogger().log("curr date: " + currDate);
            context.getLogger().log("curr time: " + currTime + " prev time: " + prevTime);

//            List<Booking> bookings = bookingRepository.findBookingsByDate(currDate.toString());
            List<Booking> bookings = bookingRepository.findBookingByDateAndTime(formattedPrevTime, formattedCurrTime);
            context.getLogger().log("All reservations for time between" +  formattedPrevTime + " to " + formattedCurrTime + " are: " + bookings);

            List<Booking> listOfFinishedBookings = getFinishedReservations(bookings);
            context.getLogger().log("List of finished reservation: " + listOfFinishedBookings);

            listOfFinishedBookings.forEach(this::sendMessageToSQS);

            resultMap.put("statusCode", 200);
            resultMap.put("message", "Reservations for date: " + currDate + " has been sent to sqs");

            return resultMap;
        } catch (Exception e) {
            context.getLogger().log(e.toString());

            resultMap.put("statusCode", 500);
            resultMap.put("message", "Internal server error");

            return resultMap;
        }

    }

    private String generateQueueUrl(String queueName) {
        try {
            GetQueueUrlRequest queueUrlRequest = new GetQueueUrlRequest()
                    .withQueueName(queueName);
            return amazonSQSClient.getQueueUrl(queueUrlRequest).getQueueUrl();
        } catch (QueueDoesNotExistException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Generate queue url: " + e.getMessage());
        }
    }

    private void sendMessageToSQS(Booking item) {
        try {
            String queueURL = generateQueueUrl(queueName);
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(queueURL)
                    .withMessageBody(new JSONObject(item).toString())
                    .withMessageAttributes(Map.of("eventType", new MessageAttributeValue().withStringValue("DAILY-EVENT").withDataType("String")));

            amazonSQSClient.sendMessage(sendMessageRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Booking> getFinishedReservations(List<Booking> bookings) {

        try {
            return bookings.stream()
                    .filter(booking -> booking.getStatus().equalsIgnoreCase("FINISHED"))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
