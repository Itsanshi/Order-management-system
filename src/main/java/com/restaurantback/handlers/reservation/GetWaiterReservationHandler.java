package com.restaurantback.handlers.reservation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.dto.reservation.WaiterReservationDTO;
import com.restaurantback.handlers.ApiResponse;
import com.restaurantback.services.BookingService;
import com.restaurantback.utils.TokenClaimsExtractor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.restaurantback.utils.TokenClaimsExtractor.email;

public class GetWaiterReservationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final BookingService bookingService;

    public GetWaiterReservationHandler(BookingService bookingService) {
        this.bookingService = bookingService;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        System.out.println("Reservation List for Waiters");



        try{
            try{
                System.out.println("Processing to Extract Email");
                email = TokenClaimsExtractor.getUserEmail(request);
                System.out.println((email == null) ? "Error in Extracting Email" : "Email: " + email);
            } catch (Exception e) {
                System.err.println("[ERROR] Authentication failed: " + e.getMessage());
                return ApiResponse.error(e);  // This will return 401 for auth errors
            }

            ZonedDateTime indianTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            LocalDate today = indianTime.toLocalDate();
            String formatDated = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

            System.out.println(formatDated);

            bookingService.updateBookings();

            Map<String, String> queryParams = request.getQueryStringParameters();
            String date = queryParams != null && queryParams.containsKey("date")
                    ? queryParams.get("date") : formatDated;

            String time = queryParams != null && queryParams.containsKey("time")
                    ? queryParams.get("time") : null;

            String tableNumber = queryParams != null && queryParams.containsKey("table")
                    ?queryParams.get("table") : null;


            System.out.println("[INFO] Extracted email: " + email);
            List<WaiterReservationDTO> waiterReservationDTOList = bookingService.getWaiterReservation(email, date, time, tableNumber);

            System.out.println(waiterReservationDTOList);

            return ApiResponse.success(waiterReservationDTOList);

        } catch (Exception e) {

            System.out.println("[ERROR] Error occurred while processing request: " + e.getMessage());

            e.printStackTrace();

            return ApiResponse.error(e);

        }
    }
}
