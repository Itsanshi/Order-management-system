package com.restaurantback.handlers.location;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.models.Location;
import com.restaurantback.services.LocationService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class AllLocationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final LocationService getAvailableLocations;
    private final ObjectMapper objectMapper;

    @Inject
    public AllLocationHandler(LocationService locationService, ObjectMapper objectMapper) {
        this.getAvailableLocations = locationService;
        this.objectMapper = objectMapper;

    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try{
            List<Location> locations = getAvailableLocations.getAllLocationsList();
            context.getLogger().log("get all locations");
            if(locations.isEmpty()){
                return createErrorResponse("No locations available at the moment.", 404);
            }

            String responseBody = objectMapper.writeValueAsString(Map.of("locations", locations));

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
