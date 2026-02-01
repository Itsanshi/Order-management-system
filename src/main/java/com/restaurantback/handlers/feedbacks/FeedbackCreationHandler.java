package com.restaurantback.handlers.feedbacks;

import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.models.Feedback;
import com.restaurantback.models.User;
import com.restaurantback.repository.BookingRepository;
import com.restaurantback.dto.ApiFeedbackDTO;
import com.restaurantback.dto.CognitoToken;
import com.restaurantback.services.FeedbackService;
import com.restaurantback.utils.GetAccessTokenFromRequest;
import com.restaurantback.utils.GetDataFromJwt;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
//
public class FeedbackCreationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final FeedbackService feedbackService;
    private final ObjectMapper objectMapper;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final BookingRepository bookingRepository ;

    private final String userTableName = System.getenv("userTable");
    @Inject
    public FeedbackCreationHandler(FeedbackService feedbackService, ObjectMapper objectMapper , BookingRepository bookingRepository) {
        this.feedbackService = feedbackService;
        this.objectMapper = objectMapper;
        this.dynamoDbEnhancedClient = DynamoDbEnhancedClient.create();
        this.bookingRepository = bookingRepository ;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Feedback feedback = new Feedback();
        String feedbackJson = "{}";
        User user = null ;
        try {

            ApiFeedbackDTO apiFeedback = objectMapper.readValue(request.getBody(), ApiFeedbackDTO.class);

            if (apiFeedback.getCuisineComment() == null || apiFeedback.getServiceComment() == null ||
                    apiFeedback.getReservationId() == null) {
                throw new IllegalArgumentException("Missing required fields");
            }

            int serviceRating = Integer.parseInt(apiFeedback.getServiceRating());
            int cuisineRating = Integer.parseInt(apiFeedback.getCuisineRating());

            if (serviceRating < 1 || serviceRating > 5 || cuisineRating < 1 || cuisineRating > 5) {
                throw new IllegalArgumentException("Ratings must be between 1 and 5");
            }

            String idToken = GetAccessTokenFromRequest.getIdToken(request);
            CognitoToken cognitoToken = GetDataFromJwt.extractDataFromToken(idToken);
            String email = cognitoToken.getEmail();
            context.getLogger().log(email);


            //  Fetch imageUrl using email
            DynamoDbTable<User> userTable = dynamoDbEnhancedClient.table(userTableName, TableSchema.fromBean(User.class));
            user = userTable.getItem(r -> r.key(k -> k.partitionValue(email)));
            String imageUrl = user != null ? user.getImageUrl() : "test";
            String fname = user != null ? user.getFirstName() : "test";
            String lname = user != null ? user.getLastName() : "test";
            String role = user != null ? user.getRole().getRoleName() : "test";

            feedback.setComment(apiFeedback.getCuisineComment() + " | " + apiFeedback.getServiceComment());
            int averageRating = (cuisineRating + serviceRating) / 2;
            feedback.setRate(String.valueOf(averageRating));
            feedback.setResID(apiFeedback.getReservationId());
            feedback.setCuisineComment(apiFeedback.getCuisineComment());
            feedback.setCuisineRating(String.valueOf(apiFeedback.getCuisineRating()));
            feedback.setServiceComment(apiFeedback.getServiceComment());
            feedback.setServiceRating(String.valueOf(apiFeedback.getServiceRating()));
            feedback.setUserAvatarUrl(imageUrl);
            feedback.setUserName(fname + " " + lname);
            feedback.setType(role);
            feedback.setLocationID(bookingRepository.getReservationLocationWithId(apiFeedback.getReservationId()));

            String dateString = Instant.now()
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            feedback.setDate(dateString);

            feedbackService.createFeedback(feedback);
            feedbackJson = objectMapper.writeValueAsString(feedback);

            bookingRepository.setFeedback(apiFeedback.getReservationId(), feedback.getId());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody("{\"feedback_id\": \"" + feedback.getId() + "\", \"message\": \"Feedback has been created\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
//                    .withBody("{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
                    .withBody("{\"error\":\"Internal Server Error: " + e.getMessage() + "\", " +
                            "\"feedback_id\":" + feedback.getId() +
                            "\"feedback_comment\":" + feedback.getComment() +
                            "\"feedback_resId\":" + feedback.getResID() +
//                            "\"feedback_cuisineRating\":" + feedback.getCuisineRating() +
//                            "\"feedback_CuisineComment\":" + String.valueOf(feedback.getCuisineRating()) +
//                            "\"feedback_serviceRating\":" + String.valueOf(feedback.getServiceRating())+
//                            "\"feedback_serviceComment\":" + feedback.getServiceComment() +
//                            "\"feedback_CuisineComment\":" + String.valueOf(feedback.getCuisineRating()) +
//                            "\"feedback_serviceRating\":" + String.valueOf(feedback.getServiceRating())+
//                            "\"feedback_serviceRating\":" + bookingRepository.getReservationLocationWithId(feedback.getResID()).getS()+
//                            "\"feedback_imageUrl\":" + user.getImageUrl() +
//                            "\"feedback_fname\":" + user.getFirstName() +
//                            "\"feedback_lname\":" + user.getLastName() +
                            "}");

        }
    }

    // id token -> email, fname, lname, role, imageUrl
    // C:\Users\amitsuresh_dange\Downloads\restaurant-back\restaurant-back
}