package com.restaurantback.handlers.reservation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.exceptions.reservationException.UnauthorizedException;
import com.restaurantback.handlers.ApiResponse;
import com.restaurantback.services.BookingService;
import com.restaurantback.utils.TokenClaimsExtractor;

import javax.validation.ValidationException;
import java.util.Map;
import java.util.Objects;

import static com.restaurantback.utils.TokenClaimsExtractor.email;

public class DeleteReservationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final BookingService bookingService;

    public DeleteReservationHandler(BookingService bookingService) {

        this.bookingService = bookingService;

    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        String email=null;
        try {

            try {

                System.out.println("Processing to Extract Email");

                email = TokenClaimsExtractor.getUserEmail(request);

                System.out.println((email == null) ? "Error in Extracting Email" : "Email: " + email);

            } catch (Exception e) {

                System.err.println("[ERROR] Authentication failed: " + e.getMessage());

                return ApiResponse.error(e);  // This will return 401 for auth errors

            }

            String reservationId = getReservationId(request);

            System.out.println("reservation_id: "+reservationId+" email: "+email+" in Handler");

            bookingService.deleteReservationWithId(reservationId, email, false);

            return ApiResponse.success(Map.of("message", "Reservation deleted successfully"));

        } catch (Exception e) {

            System.err.println("[ERROR] Error deleting reservation: " + e.getMessage());

            e.printStackTrace();

            return ApiResponse.error(e);

        }

    }

    private String getReservationId(APIGatewayProxyRequestEvent request) {

        Map<String, String> pathParameters = request.getPathParameters();

        if (pathParameters == null || !pathParameters.containsKey("id")) {

            System.err.println("[ERROR] Reservation ID is missing from path parameters");

            throw new ValidationException("Reservation ID is required");

        }

        return pathParameters.get("id");

    }

}
