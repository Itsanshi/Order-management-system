package com.restaurantback.utils;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

public class GetAccessTokenFromRequest {

    public static String getIdToken(APIGatewayProxyRequestEvent request) {
        String authorizationHeader = request.getHeaders().get("Authorization");

        if (authorizationHeader == null) {
            throw new RuntimeException("authorization header is null");
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("authorization token is not Bearer token");
        }

        return authorizationHeader.substring("Bearer ".length());

    }
}
