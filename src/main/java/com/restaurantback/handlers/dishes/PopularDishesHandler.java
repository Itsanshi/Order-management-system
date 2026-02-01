package com.restaurantback.handlers.dishes;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.models.Dish;
import com.restaurantback.services.DishesService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class PopularDishesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DishesService dishesService;
    private final ObjectMapper objectMapper ;

    public PopularDishesHandler(DishesService dishesService, ObjectMapper objectMapper) {
        this.dishesService = dishesService;
        this.objectMapper = objectMapper;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try{
            List<Dish> dishes = dishesService.getPopularDishes();
            context.getLogger().log("get all popular dishes");
            if(dishes.isEmpty()){
                return createErrorResponse("No dish available at the moment.", 404);
            }

            String responseBody = objectMapper.writeValueAsString(Map.of("dishes", dishes));

            context.getLogger().log(responseBody);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(responseBody);
        } catch (Exception e){
            context.getLogger().log(e.toString());
            return createErrorResponse("Unexpected error occurred. Please try again later.", 500);
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(String message, int statusCode){
        Map<String, Object> errorResponse = Map.of(
                "error", Map.of(
                        "message", message,
                        "statusCode", statusCode
                )
        );

        try{
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e){
//            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": {\"message\": \"Error while processing the request.\"}}");
        }
    }
}
