package com.restaurantback.handlers.reservation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.exceptions.reservationException.UnauthorizedException;
import com.restaurantback.handlers.ApiResponse;
import com.restaurantback.services.BookingService;
import com.restaurantback.utils.TokenClaimsExtractor;

import javax.inject.Inject;
import javax.validation.ValidationException;
import java.util.Map;
import java.util.Objects;

public class DeleteReservationWaiterHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final BookingService bookingService;

    @Inject
    public DeleteReservationWaiterHandler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            String reservationId = getReservationId(requestEvent);
            String waiterEmail = TokenClaimsExtractor.getUserEmail(requestEvent);

            String role = TokenClaimsExtractor.getRole(requestEvent);

            if (!role.equalsIgnoreCase("waiter"))
                throw new UnauthorizedException("Non waiter User roles Don't have the access To Modify the Reservation!");

            bookingService.deleteReservationWithId(reservationId, waiterEmail, true);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"message\": \"Reservation Cancelled successfully\"}");
        } catch (Exception e) {
            return ApiResponse.error(e);
        }
    }

    private String getReservationId(APIGatewayProxyRequestEvent request) {
        Map<String, String> pathParameters = request.getPathParameters();
        if (pathParameters == null || !pathParameters.containsKey("id")) {
            throw new ValidationException("Reservation ID is required");
        }
        return pathParameters.get("id");
    }
}