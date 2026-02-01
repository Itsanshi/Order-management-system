package com.restaurantback.handlers.dishes;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.DishDTO;
import com.restaurantback.exceptions.dishException.*;
import com.restaurantback.services.DishesService;

import javax.inject.Inject;
import java.util.Map;

public class DishesByIdHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DishesService dishesService;
    private final ObjectMapper objectMapper;

    @Inject
    public DishesByIdHandler(DishesService dishesService, ObjectMapper objectMapper) {
        this.dishesService = dishesService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            String path = requestEvent.getPath();
            String[] pathParts = path.split("/");
            String dishId = pathParts[pathParts.length - 1];

            if(dishId.equalsIgnoreCase("popular")){
                PopularDishesHandler popularDishesHandler = new PopularDishesHandler(dishesService, objectMapper);
                return popularDishesHandler.handleRequest(requestEvent, context);
            }

            context.getLogger().log("Fetching dish by id: " + dishId);

            DishDTO dish = dishesService.getDishesById(dishId);
            String responseBody = objectMapper.writeValueAsString(dish);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(responseBody);

        } catch (Exception e) {
            context.getLogger().log("Error occurred: " + e.getMessage());
            int statusCode = e instanceof DishNotFoundException ? 404 : 500;
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
