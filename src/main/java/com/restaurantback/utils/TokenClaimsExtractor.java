package com.restaurantback.utils;


import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.restaurantback.exceptions.reservationException.*;

import lombok.Getter;

import lombok.Setter;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.Value;

import java.util.Base64;

import java.util.Map;

public class TokenClaimsExtractor {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String AUTH_HEADER = "Authorization";

    public static String email;

    private TokenClaimsExtractor() {

        throw new IllegalStateException("Utility class");

    }

    public static String getUserEmail(APIGatewayProxyRequestEvent input) {

        return getClaimValue(input, "email");

    }

    public static String getUserId(APIGatewayProxyRequestEvent input) {

        return getClaimValue(input, "sub");

    }

    public static String getUsername(APIGatewayProxyRequestEvent input) {

        return getClaimValue(input, "cognito:username");

    }

    public static String getRole(APIGatewayProxyRequestEvent input){
        return getClaimValue(input,"custom:role");
    }

    private static String getClaimValue(APIGatewayProxyRequestEvent input, String claimName) {

        Map<String, Object> claims = getTokenClaims(input);

        Object value = claims.get(claimName);

        return value != null ? value.toString() : null;

    }

    private static Map<String, Object> getTokenClaims(APIGatewayProxyRequestEvent input) {

        validateInput(input);

        String token = extractToken(input.getHeaders().get(AUTH_HEADER));

        return decodeToken(token);

    }

    private static void validateInput(APIGatewayProxyRequestEvent input) {

        if (input == null || input.getHeaders() == null) {

            throw new UnauthorizedException("Missing request headers");

        }

        String authHeader = input.getHeaders().get(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {

            throw new UnauthorizedException("Missing or invalid Authorization header");

        }

    }

    private static String extractToken(String authHeader) {

        return authHeader.substring(BEARER_PREFIX.length()).trim();

    }

    private static Map<String, Object> decodeToken(String token) {

        try {

            String payload = extractPayload(token);

            byte[] decodedBytes = Base64.getDecoder().decode(payload);

            String decodedString = new String(decodedBytes);

            return mapper.readValue(decodedString, Map.class);

        } catch (Exception e) {

            System.err.println("Token decode error: " + e.getMessage());

            throw new UnauthorizedException("Invalid token format");

        }

    }

    private static String extractPayload(String token) {

        try {

            StringBuilder payload = new StringBuilder(token.split("\\.")[1]);

            payload = new StringBuilder(payload.toString().replace("-", "+").replace("_", "/"));

            while (payload.length() % 4 != 0) {

                payload.append("=");

            }

            return payload.toString();

        } catch (ArrayIndexOutOfBoundsException e) {

            throw new UnauthorizedException("Invalid token structure");

        }

    }

}
