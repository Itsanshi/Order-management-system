package com.restaurantback.handlers.tables;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.TableAvailableDTO;
import com.restaurantback.models.TimeSlot;
import com.restaurantback.services.TableService;
import com.restaurantback.utils.TimeslotDB;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AvailableTableHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final TableService tableService;
    private final ObjectMapper objectMapper;

    @Inject
    public AvailableTableHandler(TableService tableService, ObjectMapper objectMapper) {
        this.tableService = tableService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            // Extract query parameters
            Map<String, String> queryParams = apiGatewayProxyRequestEvent.getQueryStringParameters();
            if (queryParams == null || !queryParams.containsKey("locationId") || !queryParams.containsKey("date")
                    || !queryParams.containsKey("timeSlot") || !queryParams.containsKey("guests")) {
                return createErrorResponse("Missing required query parameters. Required: locationId, date, timeSlot, guests.", 400);
            }

            // Extract and validate required parameters
            String locationId = queryParams.get("locationId");
            String dateString = queryParams.get("date");
            String timeSlotString = queryParams.get("timeSlot");
            int guests;

            try {
                guests = Integer.parseInt(queryParams.get("guests"));
            } catch (NumberFormatException e) {
                return createErrorResponse("Invalid value for guests. Must be a number.", 400);
            }

            if(guests <= 0 || guests > 50){
                return createErrorResponse("Invalid guests number", 400);
            }

            // Convert date and timeSlot to appropriate types
            Date date;
            try {
                date = parseDate(dateString);
            } catch (Exception e) {
                return createErrorResponse("Invalid date format. Expected format: yyyy-MM-dd.", 400);
            }
            TimeSlot timeSlot = parseTimeSlot(timeSlotString);

            if (timeSlot == null) {
                return createErrorResponse("Invalid time slot: " + timeSlotString + ". Please provide a valid time range like '10:30-12:00'.", 400);
            }

            System.out.println(timeSlot);
            System.out.println("getting tables");

            // Fetch available tables
            List<TableAvailableDTO> availableTables = tableService.getAvailableTables(locationId, date, timeSlot, guests);

            System.out.println(availableTables);

            if(availableTables == null){
                return createErrorResponse("No location available with that id.", 404);
            }
            if (availableTables.isEmpty()) {
                return createErrorResponse("No available tables found for the specified criteria.", 404);
            }

            // Create response with available tables
            String responseBody = objectMapper.writeValueAsString(Map.of("tables", availableTables));

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Unexpected error occurred. Please try again later.", 500);
        }
    }

    // Parse date in "yyyy-MM-dd" format
    private Date parseDate(String dateString) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false); // Ensures strict format parsing

        try {
            return sdf.parse(dateString);
        } catch (Exception e) {
            // Log the error if needed
            System.err.println("Invalid date format: " + dateString);
            throw new Exception("Invalid date format. Expected format: yyyy-MM-dd");
        }
    }


    private TimeSlot parseTimeSlot(String timeSlotString) {
        System.out.println(timeSlotString);


        System.out.println("Converting to Entity");
        // Fetch TimeSlot from DB to get the ID
        TimeSlot fetchedTimeSlot = TimeslotDB.getTimeSlotFromDB(timeSlotString);

        System.out.println(fetchedTimeSlot);
        return fetchedTimeSlot;
    }

    // Create error response
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
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": {\"message\": \"Error while processing the request.\"}}");
        }
    }
}
