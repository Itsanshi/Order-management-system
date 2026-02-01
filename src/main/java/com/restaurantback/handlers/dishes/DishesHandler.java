package com.restaurantback.handlers.dishes;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.restaurantback.dto.DishDTO;
import com.restaurantback.dto.DishResponseDTO;
import com.restaurantback.dto.DishSmallDTO;
import com.restaurantback.services.DishesService;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public class DishesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DishesService dishesService;
    private final ObjectMapper objectMapper;

    @Inject
    public DishesHandler(DishesService dishesService, ObjectMapper objectMapper) {
        this.dishesService = dishesService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            Map<String, String> queryParams = Optional.ofNullable(requestEvent.getQueryStringParameters())
                    .orElse(Collections.emptyMap());

            String sort = queryParams.getOrDefault("sort", ""); // e.g., popularity:asc or price:desc
            String dishType = queryParams.getOrDefault("dishType", "");

            context.getLogger().log("Fetching dishes with sort: " + sort + ", dishType: " + dishType);
            //list of dishes
            List<DishSmallDTO> dishes = dishesService.sortDishes(sort);

            context.getLogger().log("dishes: " + dishes);
            if (!dishType.isEmpty()) {
                dishes = dishes.stream()
                        .filter( dish -> dish.getDishType()!=null && dish.getDishType().equalsIgnoreCase(dishType))
                        .collect(Collectors.toList());
            }

            context.getLogger().log("dishes: " + dishes);
            if (dishes.isEmpty()) {
                return createErrorResponse("No dishes found" + (!dishType.isEmpty() ? " for type: " + dishType : ""), 404);
            }
            List<DishResponseDTO> responseDTOS=new ArrayList<>();
            for(DishSmallDTO dishSmallDTO:dishes){
                DishResponseDTO dishResponseDTO=DishResponseDTO.builder().id(dishSmallDTO.getId())
                        .name(dishSmallDTO.getName())
                        .price(dishSmallDTO.getPrice())
                        .state(dishSmallDTO.getState())
                        .weight(dishSmallDTO.getWeight())
                        .imageUrl(dishSmallDTO.getImageUrl())
                        .build();
                responseDTOS.add(dishResponseDTO);

            }


            String responseBody = objectMapper.writeValueAsString(Map.of("dishes", responseDTOS));
            context.getLogger().log("response Body: " + responseBody);
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
        Map<String, Object> errorResponse = Map.of(
                "error", Map.of(
                        "message", message,
                        "statusCode", statusCode
                )
        );

        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":{\"message\":\"Error while processing the request\"}}");
        }
    }
}
