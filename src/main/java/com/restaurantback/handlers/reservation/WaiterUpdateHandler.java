package com.restaurantback.handlers.reservation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.reservation.ReservationDto;
import com.restaurantback.exceptions.reservationException.UnauthorizedException;
import com.restaurantback.handlers.ApiResponse;
import com.restaurantback.services.BookingService;
import com.restaurantback.utils.TokenClaimsExtractor;

import javax.inject.Inject;
import javax.validation.ValidationException;
import java.util.Map;
import java.util.Objects;

public class WaiterUpdateHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Inject
    public WaiterUpdateHandler(BookingService bookingService, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.bookingService = bookingService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        String bookingId = getBookingId(requestEvent);
        try {
            String body = requestEvent.getBody();

            if (body == null || body.isEmpty()) {
                throw new IllegalArgumentException("The Request Body cannot be Null or Empty! ");
            }

            Map<String, String> requestBody = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {
            });

            validateRequestBody(requestBody);

            String email = TokenClaimsExtractor.getUserEmail(requestEvent);

            String role = TokenClaimsExtractor.getRole(requestEvent);

            if (!role.equalsIgnoreCase("waiter"))
                throw new UnauthorizedException("Non waiter User roles Don't have the access To Modify the Reservation!");

            ReservationDto reservationDto = processUpdate(bookingId, email, requestBody);

            return ApiResponse.success(reservationDto);
        } catch (Exception e) {
            return ApiResponse.error(e);
        }
    }

    private ReservationDto processUpdate(String bookingId, String email, Map<String, String> requestBody) {

        if (isTableUpdate(requestBody) && isDateTimeUpdate(requestBody)) {

            // First update table

            bookingService.updateBookingTableWithId(

                    bookingId,

                    requestBody.get("tableId"),

                    requestBody.get("locationId")

            );

            // Then update date/time and return final state

            return bookingService.updateBookingDateAndTimeWithId(

                    bookingId,

                    email,

                    requestBody,

                    true

            );

        } else if (isTableUpdate(requestBody)) {

            return bookingService.updateBookingTableWithId(

                    bookingId,

                    requestBody.get("tableId"),

                    requestBody.get("locationId")

            );

        } else if (isDateTimeUpdate(requestBody)) {

            return bookingService.updateBookingDateAndTimeWithId(

                    bookingId,

                    email,

                    requestBody, true

            );

        }

        throw new IllegalArgumentException("Only table and date and time of the Reservation can be Updated!");

    }


    private void validateRequestBody(Map<String, String> requestBody) {
        if (requestBody.containsKey("tableId") ^ requestBody.containsKey("locationId")) {
            throw new IllegalArgumentException("tableId and locationId both are needed to update the Table for the Reservation!");
        }
    }

    private boolean isTableUpdate(Map<String, String> requestBody) {
        return requestBody.containsKey("tableId") && requestBody.containsKey("locationId");
    }

    private boolean isDateTimeUpdate(Map<String, String> requestBody) {
        return requestBody.containsKey("date") || requestBody.containsKey("timeFrom") && requestBody.containsKey("timeTo");
    }

    private String getBookingId(APIGatewayProxyRequestEvent request) {
        Map<String, String> pathParameters = request.getPathParameters();
        if (pathParameters == null || !pathParameters.containsKey("id")) {
            throw new ValidationException("Booking ID is required");
        }
        return pathParameters.get("id");
    }
}