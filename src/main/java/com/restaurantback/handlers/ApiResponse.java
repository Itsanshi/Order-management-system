package com.restaurantback.handlers;


import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.exceptions.reservationException.*;

import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;

public class ApiResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, String> getHeaders() {

        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json");

        headers.put("Access-Control-Allow-Origin", "*");

        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");

        headers.put("Access-Control-Allow-Methods", "OPTIONS,GET,POST,DELETE");

        return headers;

    }

    public static APIGatewayProxyResponseEvent success(Object body) {

        try {

            return new APIGatewayProxyResponseEvent()

                    .withStatusCode(200)

                    .withBody(objectMapper.writeValueAsString(body))

                    .withHeaders(getHeaders());

        } catch (Exception e) {

            return error(e);

        }

    }

    public static APIGatewayProxyResponseEvent error(Exception e) {

        // Debug output to see the actual exception

        System.out.println("Exception type: " + e.getClass().getName());

        System.out.println("Exception message: " + e.getMessage());

        // Handle Cognito exceptions first

        if (e instanceof UnauthorizedException || e instanceof NotAuthorizedException) {
            return createResponse(401, e.getMessage(), true);
        }

        if (e instanceof ForbiddenException) {
            return createResponse(403, e.getMessage(), false);
        }

        if (e instanceof NotFoundException) {
            return createResponse(404, e.getMessage(), false);
        }

        // Handle validation exceptions

        if (e instanceof ValidationException || e instanceof IllegalArgumentException) {
            return createResponse(400, e.getMessage(), false);
        }

        if (e instanceof ConflictException) {
            return createResponse(409, e.getMessage(), false);
        }

        // If we get here, it's an unexpected error

        return createResponse(500, "Internal server error: " + e.getClass().getSimpleName(), false);

    }

    private static APIGatewayProxyResponseEvent createResponse(int statusCode, String message, boolean isAuthError) {

        Map<String, String> headers = getHeaders();

        if (isAuthError) {

            headers.put("WWW-Authenticate", "Bearer error=\"invalid_token\"");

        }

        try {

            Map<String, Object> responseBody = new HashMap<>();

            responseBody.put("statusCode", statusCode);

            responseBody.put("message", message);

            return new APIGatewayProxyResponseEvent()

                    .withStatusCode(statusCode)

                    .withBody(objectMapper.writeValueAsString(responseBody))

                    .withHeaders(headers);

        } catch (Exception e) {

            // Fallback if JSON serialization fails

            return new APIGatewayProxyResponseEvent()

                    .withStatusCode(500)

                    .withBody("{\"statusCode\":500,\"error\":\"Failed to generate error response\"}")

                    .withHeaders(headers);

        }

    }

}
