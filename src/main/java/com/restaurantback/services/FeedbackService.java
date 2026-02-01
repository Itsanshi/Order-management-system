package com.restaurantback.services;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.restaurantback.dto.FeedbackDTO;
import com.restaurantback.dto.PaginatedResponse;
import com.restaurantback.dto.report.FeedbackUpdate;
import com.restaurantback.models.Booking;
import com.restaurantback.models.Feedback;
import org.json.JSONObject;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

public class FeedbackService {
    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;
    private final String queueName = System.getenv("reportQueue");
    private final AmazonSQS amazonSQSClient;

    public FeedbackService(DynamoDbEnhancedClient dynamoDbEnhancedClient, AmazonSQS amazonSQSClient) {
        this.enhancedClient = dynamoDbEnhancedClient;
        this.tableName = System.getenv("feedbackTable"); // Change this to your actual table name
        this.amazonSQSClient = amazonSQSClient;
    }

    public PaginatedResponse<FeedbackDTO> getFeedbackByLocationWithPagination(String locationId, int limit, int page, String type, String sortBy) {
        DynamoDbTable<Feedback> table = enhancedClient.table(tableName, TableSchema.fromBean(Feedback.class));

        List<Feedback> filteredFeedbacks = table.scan().items().stream()
                .filter(fb -> locationId.equals(fb.getLocationID()))
                .filter(fb -> type == null || type.isEmpty() || type.equalsIgnoreCase(fb.getType()))
                .sorted((f1, f2) -> {
                    try {
                        return switch (sortBy.toLowerCase()) {
                            case "desc" ->
                                    Double.compare(Double.parseDouble(f2.getRate()), Double.parseDouble(f1.getRate()));
                            case "asc" ->
                                    Double.compare(Double.parseDouble(f1.getRate()), Double.parseDouble(f2.getRate()));
                            default -> f2.getDate().compareTo(f1.getDate());
                        };
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .toList();

        List<FeedbackDTO> feedbackDTOS = filteredFeedbacks.stream()
                .map(feedback -> {
                    return FeedbackDTO.builder()
                            .id(feedback.getId())
                            .date(feedback.getDate())
                            .comment(feedback.getComment())
                            .rate(feedback.getRate())
                            .locationId(feedback.getLocationID())
                            .type(feedback.getType())
                            .userName(feedback.getUserName())
                            .userAvatarUrl(feedback.getUserAvatarUrl())
                            .build();
                })
                .toList();

        int totalElements = filteredFeedbacks.size();
        int totalPages = (int) Math.ceil((double) totalElements / limit);
        int offset = page * limit;

        List<FeedbackDTO> pagedContent = feedbackDTOS.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        PaginatedResponse<FeedbackDTO> response = new PaginatedResponse<>();
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setSize(limit);
        response.setNumber(page);
        response.setNumberOfElements(pagedContent.size());
        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1);
        response.setEmpty(pagedContent.isEmpty());
        response.setContent(pagedContent);

        PaginatedResponse.Pageable pageable = new PaginatedResponse.Pageable();
        pageable.setOffset(offset);
        pageable.setPageNumber(page);
        pageable.setPageSize(limit);
        pageable.setPaged(true);
        pageable.setUnpaged(false);

        PaginatedResponse.Sort sort = new PaginatedResponse.Sort();
        sort.setDirection(sortBy.equalsIgnoreCase("oldest") || sortBy.equalsIgnoreCase("worst") ? "ASC" : "DESC");
        sort.setProperty(sortBy.equalsIgnoreCase("best") || sortBy.equalsIgnoreCase("worst") ? "rate" : "date");
        sort.setAscending(sortBy.equalsIgnoreCase("oldest") || sortBy.equalsIgnoreCase("worst"));
        sort.setIgnoreCase(false);
        sort.setNullHandling("NATIVE");

        pageable.setSort(List.of(sort));
        response.setPageable(pageable);
        response.setSort(List.of(sort));

        return response;
    }

    public void createFeedback(Feedback feedback){
        DynamoDbTable<Feedback> table = enhancedClient.table(tableName, TableSchema.fromBean(Feedback.class));
        try {
            if (feedback.getId() == null || feedback.getId().isEmpty()) {
                feedback.setId(UUID.randomUUID().toString());
            }
            if (feedback.getDate() == null || feedback.getDate().isEmpty()) {
                feedback.setDate(Instant.now()
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (feedback.getLocationID() == null || feedback.getComment() == null || feedback.getRate() == null) {
                throw new IllegalArgumentException("locationID, comment, and rate are required");
            }

            System.out.println(feedback);
            table.putItem(feedback);
            System.out.println("feedback has been saved.");
            sendMessageToSQSCreateEvent(feedback);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create feedback: " + e.getMessage(), e);
        }
    }

    public Feedback getFeedbackByReservationId(String reservationId) {
        DynamoDbTable<Feedback> table = enhancedClient.table(tableName, TableSchema.fromBean(Feedback.class));
        Key key = Key.builder().partitionValue(reservationId).build();
        return table.getItem(key);
    }


    public Feedback getFeedbackById(String feedbackId) {
        DynamoDbTable<Feedback> table = enhancedClient.table(tableName, TableSchema.fromBean(Feedback.class));

        Key key = Key.builder()
                .partitionValue(feedbackId)
                .build();

        Feedback feedback = table.getItem(r -> r.key(key));

        if (feedback == null) {
            throw new RuntimeException("Feedback doesn't exist with the ID: " + feedbackId);
        }

        return feedback;
    }

    public void updateFeedback(Feedback feedback) {
        DynamoDbTable<Feedback> table = enhancedClient.table(tableName, TableSchema.fromBean(Feedback.class));
        Key key = Key.builder()
                .partitionValue(feedback.getId())
                .build();
        Feedback prevFeedback = table.getItem(key);
        System.out.println(prevFeedback);
        System.out.println(feedback);
        FeedbackUpdate feedbackUpdate = new FeedbackUpdate();
        feedbackUpdate.setPreviousFeedback(prevFeedback);
        feedbackUpdate.setFeedback(feedback);
        sendMessageToSQSUpdateEvent(feedbackUpdate);

        table.putItem(feedback);
    }

    private void sendMessageToSQSCreateEvent(Feedback item) {
        try {
            String queueURL = generateQueueUrl(queueName);
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(queueURL)
                    .withMessageBody(new JSONObject(item).toString())
                    .withMessageAttributes(Map.of("eventType", new MessageAttributeValue().withStringValue("NEW_FEEDBACK_EVENT").withDataType("String")));

            amazonSQSClient.sendMessage(sendMessageRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessageToSQSUpdateEvent(FeedbackUpdate item) {
        try {
            String queueURL = generateQueueUrl(queueName);
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(queueURL)
                    .withMessageBody(new JSONObject(item).toString())
                    .withMessageAttributes(Map.of("eventType", new MessageAttributeValue().withStringValue("FEEDBACK_EVENT").withDataType("String")));

            amazonSQSClient.sendMessage(sendMessageRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
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


}