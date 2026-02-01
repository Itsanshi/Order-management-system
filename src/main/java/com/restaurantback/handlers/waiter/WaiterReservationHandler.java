package com.restaurantback.handlers.waiter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.ReservationByWaiterDTO;
import com.restaurantback.dto.WaiterBookingDTO;
import com.restaurantback.handlers.ApiResponse;
import com.restaurantback.services.ReservationService;
import com.restaurantback.utils.TokenClaimsExtractor;
import org.json.JSONObject;

import java.time.*;
import java.util.Map;
import java.util.UUID;

import static com.restaurantback.utils.TokenClaimsExtractor.email;

public class WaiterReservationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    public WaiterReservationHandler(ReservationService reservationService, ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            try {
                System.out.println("Processing to Extract Email");
                email = TokenClaimsExtractor.getUserEmail(request);
                System.out.println((email == null) ? "Error in Extracting Email" : "Email: " + email);
            } catch (Exception e) {
                System.err.println("[ERROR] Authentication failed: " + e.getMessage());
                return ApiResponse.error(e);
            }

            System.out.println("Email extracted : " + email);

            WaiterBookingDTO waiterBookingDTO = objectMapper.readValue(request.getBody(), WaiterBookingDTO.class);

            if (waiterBookingDTO.getClientType() == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(Map.of("Content-Type", "application/json"))
                        .withBody("{\"message\": \"clientType is required.\"}");
            }

            String dateStr = waiterBookingDTO.getDate();
            String timeStr = waiterBookingDTO.getTimeFrom();

            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime inputDateTime = LocalDateTime.of(date, time);

            ZoneId zone = ZoneId.of("Asia/Kolkata");
            ZonedDateTime inputZonedDateTime = inputDateTime.atZone(zone);

            ZonedDateTime now = ZonedDateTime.now(zone);

            if (inputZonedDateTime.isBefore(now) || inputZonedDateTime.isEqual(now)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(Map.of("Content-Type", "application/json"))
                        .withBody("{\"message\": \"Date and time not valid\"}");
            }

            String clientType = waiterBookingDTO.getClientType().toLowerCase();

            switch (clientType) {
                case "customer":
                    if (waiterBookingDTO.getClientEmail() == null || waiterBookingDTO.getClientEmail().isBlank()) {
                        return new APIGatewayProxyResponseEvent()
                                .withStatusCode(400)
                                .withHeaders(Map.of("Content-Type", "application/json"))
                                .withBody("{\"message\": \"Customer email is required for clientType 'customer'.\"}");
                    }
                    break;

                case "visitor":
                    // Optionally: clear customerEmail if it was sent accidentally
                    waiterBookingDTO.setClientEmail(null);
                    break;

                default:
                    return new APIGatewayProxyResponseEvent()
                            .withStatusCode(400)
                            .withHeaders(Map.of("Content-Type", "application/json"))
                            .withBody("{\"message\": \"Invalid clientType. Must be 'customer' or 'visitor'.\"}");
            }


            ReservationByWaiterDTO reservation = reservationService.bookTable(email, waiterBookingDTO, UUID.randomUUID().toString());

            String responseBody = objectMapper.writeValueAsString(reservation);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(responseBody);
        } catch (Exception e) {
            System.err.println("Error processing booking: " + e.getMessage());
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(new JSONObject().put("message", e.getMessage()).toString());
        }
    }
}
