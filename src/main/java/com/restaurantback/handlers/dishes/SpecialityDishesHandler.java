package com.restaurantback.handlers.dishes;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.DishInfoDTO;
import com.restaurantback.services.LocationService;

import java.util.Map;

public class SpecialityDishesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final LocationService locationService;
    private final ObjectMapper objectMapper;

    public SpecialityDishesHandler(LocationService locationService, ObjectMapper objectMapper) {
        this.locationService = locationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String locationId = request.getPathParameters().get("id");
            if (locationId == null || locationId.isBlank()) {
                return createErrorResponse("Missing or invalid location ID.", 400);
            }

            context.getLogger().log("Fetching specialty dishes for location ID: " + locationId);
            DishInfoDTO specialityDishes = locationService.getSpecialityDishesByLocationId(locationId);

            if (specialityDishes == null) {
                return createErrorResponse("No specialty dishes found for the given location.", 404);
            }

            String responseBody = objectMapper.writeValueAsString(Map.of("specialityDishes", specialityDishes));
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(responseBody);
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return createErrorResponse("Unexpected error occurred. Please try again later.", 500);
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(String message, int statusCode) {
        try {
            Map<String, Object> errorResponse = Map.of("error", Map.of("message", message, "statusCode", statusCode));
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": {\"message\": \"Error while processing the request.\"}}");
        }
    }
}

