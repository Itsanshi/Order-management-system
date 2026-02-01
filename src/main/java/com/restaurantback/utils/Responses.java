package com.restaurantback.utils;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Responses {
    public APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String error, List<String> messages) {

        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withBody(new JSONObject()
                            .put("messages", messages)
                            .put("error", error)
                            .toString())
                    .withHeaders(Map.of("Content-Type", "application/json"));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":\"Internal Server Error\",\"messages\":[\"We apologize, but we encountered an unexpected error. Please try again later.\"]}");
        }
    }

    public APIGatewayProxyResponseEvent createSuccessResponse(String message, int statusCode) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withBody(
                            new JSONObject()
                                    .put("message", message)
                                    .toString()
                    )
                    .withHeaders(Map.of("Content-Type", "application/json"));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("{\"error\":\"Internal Server Error\",\"messages\":[\"We apologize, but we encountered an unexpected error. Please try again later.\"]}");
        }
    }
}
