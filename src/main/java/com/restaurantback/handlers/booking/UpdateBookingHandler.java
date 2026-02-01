package com.restaurantback.handlers.booking;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.reservation.ReservationDto;
import com.restaurantback.handlers.ApiResponse;
import com.restaurantback.utils.TokenClaimsExtractor;
import com.restaurantback.services.BookingService;
import javax.inject.Inject;
import javax.validation.ValidationException;
import java.util.Map;


public class UpdateBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Inject
    public UpdateBookingHandler(ObjectMapper objectMapper,BookingService bookingService) {
        this.bookingService = bookingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        String bookingId = getBookingId(requestEvent);
        try {
            // Convert JSON string to Map<String,String>
            Map<String, String> bodyMap = objectMapper.readValue(
                    requestEvent.getBody(),
                    new TypeReference<Map<String, String>>() {
                    }
            );



            System.out.println("updatedFields in UpdateBookingHandler: "+bodyMap);

            ReservationDto reservationDto = bookingService.updateBookingDateAndTimeWithId(
                    bookingId,
                    TokenClaimsExtractor.getUserEmail(requestEvent),
                    bodyMap,false
            );

            return ApiResponse.success(reservationDto);
        }catch (Exception e) {
            return ApiResponse.error(e);
        }
    }

    private String getBookingId(APIGatewayProxyRequestEvent request) {
        Map<String, String> pathParameters = request.getPathParameters();
        if (pathParameters == null || !pathParameters.containsKey("id")) {
            throw new ValidationException("Booking ID is required");
        }
        return pathParameters.get("id");
    }
}

