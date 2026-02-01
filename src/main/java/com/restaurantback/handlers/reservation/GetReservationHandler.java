package com.restaurantback.handlers.reservation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.reservation.ReservationDto;
import com.restaurantback.handlers.ApiResponse;
import com.restaurantback.services.BookingService;
import com.restaurantback.utils.TokenClaimsExtractor;

import javax.inject.Inject;
import java.util.List;

import static com.restaurantback.utils.TokenClaimsExtractor.email;

public class GetReservationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final BookingService bookingService;

    @Inject

    public GetReservationHandler(ObjectMapper objectMapper, BookingService bookingService) {

        this.bookingService = bookingService;

    }

    @Override

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        System.out.println("[INFO] Received request to get reservations");

        bookingService.updateBookings();

        try {

            try {

                System.out.println("Processing to Extract Email");

                email = TokenClaimsExtractor.getUserEmail(input);

                System.out.println((email == null) ? "Error in Extracting Email" : "Email: " + email);

            } catch (Exception e) {

                System.err.println("[ERROR] Authentication failed: " + e.getMessage());

                return ApiResponse.error(e);  // This will return 401 for auth errors

            }


            System.out.println("[INFO] Extracted email: " + email);

            //check if email belongs to customer or waiter

            // Fetch the list of reservations for the user

            List<ReservationDto> reservationDtoList = bookingService.getListReservationDto(email);

            System.out.println("[INFO] Successfully fetched reservations for email: " + email);

            return ApiResponse.success(reservationDtoList);

        } catch (Exception e) {

            System.out.println("[ERROR] Error occurred while processing request: " + e.getMessage());

            e.printStackTrace();

            return ApiResponse.error(e);

        }

    }

}
