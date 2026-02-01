package com.restaurantback.handlers.feedbacks;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.FeedbackDTO;
import com.restaurantback.dto.PaginatedResponse;
import com.restaurantback.models.Feedback;
import com.restaurantback.services.FeedbackService;
import com.restaurantback.services.LocationService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Inject;
import java.util.Map;

public class FeedbackHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final FeedbackService feedbackService;
    private final ObjectMapper objectMapper;
    private final LocationService locationService;

    @Inject
    public FeedbackHandler(FeedbackService feedbackService, ObjectMapper objectMapper, LocationService locationService) {
        this.feedbackService = feedbackService;
        this.objectMapper = objectMapper;
        this.locationService = locationService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> queryParams = request.getQueryStringParameters();
            Map<String, String> pathParams = request.getPathParameters();

            String locationId = pathParams != null ? pathParams.get("id") : null;
            if (locationId == null || locationId.isBlank()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\":\"Missing path parameter 'id'\"}");
            }

            if(!locationService.doesLocationExist(locationId)){
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(403)
                        .withBody("Invalid Location");
            }

            int page = queryParams != null && queryParams.containsKey("page")
                    ? Integer.parseInt(queryParams.get("page"))
                    : 0;

            int limit = queryParams != null && queryParams.containsKey("limit")
                    ? Integer.parseInt(queryParams.get("limit"))
                    : 10;

            String type = queryParams != null ? queryParams.get("type") : null;

            String sortBy = queryParams != null ? queryParams.get("sortBy") : "asc";

            PaginatedResponse<FeedbackDTO> feedbackPage = feedbackService.getFeedbackByLocationWithPagination(locationId, limit, page, type, sortBy);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(objectMapper.writeValueAsString(feedbackPage));

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }
}
