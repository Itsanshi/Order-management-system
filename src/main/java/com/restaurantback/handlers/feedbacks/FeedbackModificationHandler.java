package com.restaurantback.handlers.feedbacks;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.ApiFeedbackDTO;
import com.restaurantback.dto.ApiFeedbackModificationDTO;
import com.restaurantback.models.Feedback;
import com.restaurantback.services.FeedbackService;

import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class FeedbackModificationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final FeedbackService feedbackService;
    private final ObjectMapper objectMapper;

    @Inject
    public FeedbackModificationHandler(FeedbackService feedbackService, ObjectMapper objectMapper) {
        this.feedbackService = feedbackService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            // Parse input JSON
            ApiFeedbackModificationDTO apiFeedback = objectMapper.readValue(request.getBody(), ApiFeedbackModificationDTO.class);
            String feedbackId = apiFeedback.getFeedbackId();

            if (feedbackId == null || feedbackId.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\":\"Missing 'feedbackId' in request body\"}");
            }

            // Validate required fields
            if (apiFeedback.getCuisineComment() == null || apiFeedback.getServiceComment() == null ||
                    Integer.parseInt(apiFeedback.getServiceRating()) < 1 || Integer.parseInt(apiFeedback.getServiceRating()) > 5 ||
                    Integer.parseInt(apiFeedback.getCuisineRating()) < 1 || Integer.parseInt(apiFeedback.getCuisineRating()) > 5) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\":\"Invalid feedback data: cuisineComment, serviceComment, serviceRating (1-5), and cuisineRating (1-5) are required\"}");
            }

            // Fetch feedback using reservationId
            Feedback existingFeedback = feedbackService.getFeedbackByReservationId(feedbackId);
            if (existingFeedback == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("{\"error\":\"Feedback not found for the given feedbackId\"}");
            }

            // Update feedback model
            existingFeedback.setComment(apiFeedback.getCuisineComment() + " | " + apiFeedback.getServiceComment());
            int serviceRating = Integer.parseInt(apiFeedback.getServiceRating());
            int cuisineRating = Integer.parseInt(apiFeedback.getCuisineRating());
            int averageRating = (cuisineRating + serviceRating) / 2;
            existingFeedback.setRate(String.valueOf(averageRating));
            existingFeedback.setCuisineComment(apiFeedback.getCuisineComment());
            existingFeedback.setCuisineRating(String.valueOf(apiFeedback.getCuisineRating()));
            existingFeedback.setServiceComment(apiFeedback.getServiceComment());
            existingFeedback.setServiceRating(String.valueOf(apiFeedback.getServiceRating()));
            existingFeedback.setDate(Instant.now().atZone(java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            feedbackService.updateFeedback(existingFeedback);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{\"message\":\"Feedback has been updated successfully\"}");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }
}
