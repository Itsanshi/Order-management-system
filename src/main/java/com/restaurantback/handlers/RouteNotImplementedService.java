package com.restaurantback.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public class RouteNotImplementedService implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        String path = requestEvent.getPath();
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody("{\"error\": \"Route not implemented: " + path + "\"}");
    }
}

