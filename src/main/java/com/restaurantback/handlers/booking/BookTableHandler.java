package com.restaurantback.handlers.booking;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.ReservationDtoWithTableId;
import com.restaurantback.dto.reservation.BookingDto;
import com.restaurantback.dto.reservation.ReservationDto;
import com.restaurantback.handlers.ApiResponse;
import com.restaurantback.services.BookingService;
import com.restaurantback.utils.TokenClaimsExtractor;

import javax.inject.Inject;
import java.util.UUID;

import static com.restaurantback.utils.TokenClaimsExtractor.email;

public class BookTableHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Inject
    public BookTableHandler(BookingService bookingService, ObjectMapper objectMapper) {
        this.bookingService = bookingService;

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
                return ApiResponse.error(e);  // This will return 401 for auth errors
            }

            System.out.println("Incoming request: " + request);
            BookingDto booking = objectMapper.readValue(request.getBody(), BookingDto.class);
            booking.setUserEmail(email);

            ReservationDto reservationDto = bookingService.createReservation(booking,UUID.randomUUID().toString());
            ReservationDtoWithTableId reservationDtoWithTableId = objectMapper.convertValue(reservationDto,ReservationDtoWithTableId.class);
            reservationDtoWithTableId.setTableId(booking.getTableId());

            return ApiResponse.success(reservationDtoWithTableId);

        } catch (Exception e) {
            System.err.println("Error processing booking: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error(e);
        }
    }

}
