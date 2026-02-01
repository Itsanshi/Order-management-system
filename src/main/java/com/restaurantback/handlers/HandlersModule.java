package com.restaurantback.handlers;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.RouteKey;
import com.restaurantback.handlers.auth.PostLogOutHandler;
import com.restaurantback.handlers.auth.PostRefreshTokenHandler;
import com.restaurantback.handlers.auth.PostSignInHandler;
import com.restaurantback.handlers.auth.PostSignUpHandler;
import com.restaurantback.handlers.booking.BookTableHandler;
import com.restaurantback.handlers.booking.UpdateBookingHandler;
import com.restaurantback.handlers.dishes.DishesByIdHandler;
import com.restaurantback.handlers.dishes.DishesHandler;
import com.restaurantback.handlers.dishes.PopularDishesHandler;
import com.restaurantback.handlers.dishes.SpecialityDishesHandler;
import com.restaurantback.handlers.employee.PostEmployeeHandler;
import com.restaurantback.handlers.feedbacks.FeedbackCreationHandler;
import com.restaurantback.handlers.feedbacks.FeedbackHandler;
import com.restaurantback.handlers.feedbacks.FeedbackModificationHandler;
import com.restaurantback.handlers.location.AllLocationHandler;
import com.restaurantback.handlers.location.AvailableLocationHandler;
import com.restaurantback.handlers.profile.GetUsersProfileHandler;
import com.restaurantback.handlers.profile.PutChangeUsersProfilePasswordHandler;
import com.restaurantback.handlers.profile.PutUsersProfileHandler;
import com.restaurantback.handlers.reservation.*;
import com.restaurantback.handlers.tables.AvailableTableHandler;
import com.restaurantback.handlers.waiter.WaiterReservationHandler;
import com.restaurantback.repository.BookingRepository;
import com.restaurantback.services.*;
import com.restaurantback.services.BookingService;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import javax.inject.Named;
import javax.inject.Singleton;
import java.awt.print.Book;
import java.util.HashMap;
import java.util.Map;

@Module
public class HandlersModule {

    @Singleton
    @Provides
    @Named("allLocationHandler")
    AllLocationHandler providesAllLocationHandler(@Named("locationService") LocationService locationService, @Named("objectMapper") ObjectMapper objectMapper) {
        return new AllLocationHandler(locationService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("availableLocationHandler")
    AvailableLocationHandler availableLocationHandler(@Named("locationService") LocationService locationService, @Named("objectMapper") ObjectMapper objectMapper) {
        return new AvailableLocationHandler(locationService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("availableTableHandler")
    AvailableTableHandler availableTableHandler(@Named("tableService") TableService tableService, @Named("objectMapper") ObjectMapper objectMapper) {
        return new AvailableTableHandler(tableService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("feedbackHandler")
    FeedbackHandler feedbackHandler(@Named("feedbackService") FeedbackService feedbackService, @Named("objectMapper") ObjectMapper objectMapper, @Named("locationService")LocationService locationService) {
        return new FeedbackHandler(feedbackService, objectMapper, locationService);
    }

    @Singleton
    @Provides
    @Named("dishesHandler")
    DishesHandler dishesHandler(@Named("dishesService") DishesService dishesService , @Named("objectMapper") ObjectMapper objectMapper){
        return new DishesHandler(dishesService,objectMapper);
    }

    @Singleton
    @Provides
    @Named("dishesByIdHandler")
    DishesByIdHandler dishesByIdHandler(@Named("dishesService") DishesService dishesService, @Named("objectMapper") ObjectMapper objectMapper){
        return new DishesByIdHandler(dishesService,objectMapper);
    }


    @Singleton
    @Provides
    @Named("specialityDishesHandler")
    SpecialityDishesHandler specialityDishesHandler(@Named("locationService") LocationService locationService, @Named("objectMapper") ObjectMapper objectMapper) {
        return new SpecialityDishesHandler(locationService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("popularDishesHandler")
    PopularDishesHandler popularDishesHandler(@Named("dishesService") DishesService dishesService, @Named("objectMapper") ObjectMapper objectMapper) {
        return new PopularDishesHandler(dishesService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("signInHandler")
    PostSignInHandler providePostSingInHandler(@Named("cognitoClient") CognitoIdentityProviderClient cognitoClient) {
        return new PostSignInHandler(cognitoClient);
    }

    @Singleton
    @Provides
    @Named("signUpHandler")
    PostSignUpHandler providePostSingUpHandler(@Named("cognitoClient") CognitoIdentityProviderClient cognitoClient, @Named("userService") UserService userService) {
        return new PostSignUpHandler(cognitoClient, userService);
    }

    @Singleton
    @Provides
    @Named("logOutHandler")
    PostLogOutHandler providePostLogOutHandler(@Named("cognitoClient") CognitoIdentityProviderClient cognitoClient) {
        return new PostLogOutHandler(cognitoClient);
    }

    @Singleton
    @Provides
    @Named("refreshTokenHandler")
    PostRefreshTokenHandler providePostRefreshTokenHandler(@Named("cognitoClient") CognitoIdentityProviderClient cognitoClient) {
        return new PostRefreshTokenHandler(cognitoClient);
    }

    @Singleton
    @Provides
    @Named("getUsersProfileHandler")
    GetUsersProfileHandler provideGetUsersProfileHandler(@Named("profileService") ProfileService profileService, @Named("objectMapper") ObjectMapper objectMapper) {
        return new GetUsersProfileHandler(profileService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("putChangeUsersProfilePasswordHandler")
    PutChangeUsersProfilePasswordHandler providePutChangeUsersProfilePasswordHandler(@Named("cognitoClient") CognitoIdentityProviderClient cognitoClient, @Named("objectMapper") ObjectMapper objectMapper) {
        return new PutChangeUsersProfilePasswordHandler(cognitoClient, objectMapper);
    }

    @Singleton
    @Provides
    @Named("putUsersProfileHandler")
    PutUsersProfileHandler providePutUsersProfileHandler(@Named("profileService") ProfileService profileService, @Named("objectMapper") ObjectMapper objectMapper) {
        return new PutUsersProfileHandler(profileService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("bookTableHandler")
    BookTableHandler bookTableHandler(@Named("bookingService") BookingService bookingService, @Named("objectMapper") ObjectMapper objectMapper) {

        return new BookTableHandler(bookingService, objectMapper);

    }

    @Singleton
    @Provides
    @Named("deleteReservationHandler")
    DeleteReservationHandler deleteReservationHandler(@Named("bookingService") BookingService bookingService) {

        return new DeleteReservationHandler(bookingService);

    }

    @Singleton
    @Provides
    @Named("getReservationHandler")
    GetReservationHandler getReservationHandler(@Named("objectMapper") ObjectMapper objectMapper, @Named("bookingService") BookingService bookingService) {

        return new GetReservationHandler(objectMapper, bookingService);

    }

    @Singleton
    @Provides
    @Named("postEmployeeHandler")
    PostEmployeeHandler providePostEmployeeHandler(@Named("employeeService") EmployeeService employeeService) {
        return new PostEmployeeHandler(employeeService);
    }

    @Singleton
    @Provides
    @Named("updateBookingHandler")
    UpdateBookingHandler updateBookingHandler(@Named("objectMapper")ObjectMapper objectMapper, @Named("bookingService")BookingService bookingService){
        return new UpdateBookingHandler(objectMapper, bookingService);
    }

    @Singleton
    @Provides
    @Named("deleteReservationWaiterHandler")
    DeleteReservationWaiterHandler deleteReservationWaiterHandler(@Named("bookingService")BookingService bookingService){
        return new DeleteReservationWaiterHandler(bookingService);
    }

    @Singleton
    @Provides
    @Named("getWaiterReservationHandler")
    GetWaiterReservationHandler getWaiterReservationHandler(@Named("bookingService")BookingService bookingService){
        return new GetWaiterReservationHandler(bookingService);
    }

    @Singleton
    @Provides
    @Named("waiterUpdateHandler")
    WaiterUpdateHandler waiterUpdateHandler(@Named("bookingService")BookingService bookingService, @Named("objectMapper")ObjectMapper objectMapper){
        return new WaiterUpdateHandler(bookingService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("waiterReservationHandler")
    WaiterReservationHandler waiterReservationHandler(@Named("reservationService") ReservationService reservationService,
                                                      @Named("objectMapper") ObjectMapper objectMapper){
        return new WaiterReservationHandler(reservationService, objectMapper);
    }

    @Singleton
    @Provides
    @Named("feedbackCreationHandler")
    FeedbackCreationHandler provideFeedbackCreationHandler(@Named("feedbackService") FeedbackService feedbackService,@Named("objectMapper")ObjectMapper objectMapper, @Named("bookingRepository") BookingRepository bookingRepository){
        return new FeedbackCreationHandler(feedbackService,objectMapper, bookingRepository);
    }

    @Singleton
    @Provides
    @Named("feedbackModificationHandler")
    FeedbackModificationHandler provideFeedbackModificationHandler(@Named("feedbackService")FeedbackService feedbackService, @Named("objectMapper")ObjectMapper objectMapper){
        return new FeedbackModificationHandler(feedbackService,objectMapper);
    }




    @Singleton
    @Provides
    @Named("routeMap")
    Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> provideHandlersByRouteKey(
            @Named("allLocationHandler") AllLocationHandler allLocationHandler,
            @Named("availableLocationHandler") AvailableLocationHandler availableLocationHandler,
            @Named("availableTableHandler") AvailableTableHandler availableTableHandler,
            @Named("feedbackHandler") FeedbackHandler feedbackHandler,
            @Named("feedbackCreationHandler") FeedbackCreationHandler feedbackCreationHandler,
            @Named("feedbackModificationHandler") FeedbackModificationHandler feedbackModificationHandler,
            @Named("specialityDishesHandler") SpecialityDishesHandler specialityDishesHandler,
            @Named("popularDishesHandler") PopularDishesHandler popularDishesHandler,
            @Named("signInHandler") PostSignInHandler postSignInHandler,
            @Named("signUpHandler") PostSignUpHandler postSignUpHandler,
            @Named("logOutHandler") PostLogOutHandler postLogOutHandler,
            @Named("getUsersProfileHandler") GetUsersProfileHandler getUsersProfileHandler,
            @Named("putChangeUsersProfilePasswordHandler") PutChangeUsersProfilePasswordHandler putChangeUsersProfilePasswordHandler,
            @Named("putUsersProfileHandler") PutUsersProfileHandler putUsersProfileHandler,
            @Named("refreshTokenHandler") PostRefreshTokenHandler postRefreshTokenHandler,
            @Named("bookTableHandler") BookTableHandler bookTableHandler,
            @Named("deleteReservationHandler") DeleteReservationHandler deleteReservationHandler,
            @Named("getReservationHandler") GetReservationHandler getReservationHandler,
            @Named("postEmployeeHandler") PostEmployeeHandler postEmployeeHandler,
            @Named("updateBookingHandler")UpdateBookingHandler updateBookingHandler,
            @Named("deleteReservationWaiterHandler")DeleteReservationWaiterHandler deleteReservationWaiterHandler,
            @Named("getWaiterReservationHandler") GetWaiterReservationHandler getWaiterReservationHandler,
            @Named("waiterUpdateHandler") WaiterUpdateHandler waiterUpdateHandler,
            @Named("waiterReservationHandler") WaiterReservationHandler waiterReservationHandler,
            @Named("dishesHandler") DishesHandler dishesHandler,
            @Named("dishesByIdHandler") DishesByIdHandler dishesByIdHandler

    ) {
        Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> routeMap = new HashMap<>();

        routeMap.put(new RouteKey("GET", "/dishes/popular"), popularDishesHandler);
        routeMap.put(new RouteKey("GET", "/dishes/{id}"), dishesByIdHandler);
        routeMap.put(new RouteKey("GET", "/dishes"), dishesHandler);
        routeMap.put(new RouteKey("GET", "/locations/select-options"), availableLocationHandler);
        routeMap.put(new RouteKey("GET", "/locations"), allLocationHandler);
        routeMap.put(new RouteKey("GET", "/locations/{id}/speciality-dishes"), specialityDishesHandler);
        routeMap.put(new RouteKey("GET", "/locations/{id}/feedbacks"), feedbackHandler);
        routeMap.put(new RouteKey("GET", "/bookings/tables"), availableTableHandler);
        routeMap.put(new RouteKey("POST", "/auth/sign-up"), postSignUpHandler);
        routeMap.put(new RouteKey("POST", "/auth/sign-in"), postSignInHandler);
        routeMap.put(new RouteKey("POST", "/auth/sign-out"), postLogOutHandler);
        routeMap.put(new RouteKey("POST", "/auth/new-access-token"), postRefreshTokenHandler);
        routeMap.put(new RouteKey("GET", "/reservations"), getReservationHandler);
        routeMap.put(new RouteKey("DELETE", "/reservations/{id}"), deleteReservationHandler);
        routeMap.put(new RouteKey("POST", "/bookings/client"), bookTableHandler);
        routeMap.put(new RouteKey("GET", "/users/profile"), getUsersProfileHandler);
        routeMap.put(new RouteKey("PUT", "/users/profile/password"), putChangeUsersProfilePasswordHandler);
        routeMap.put(new RouteKey("PUT", "/users/profile"), putUsersProfileHandler);
        routeMap.put(new RouteKey("POST", "/employee"), postEmployeeHandler);
        routeMap.put(new RouteKey("PATCH", "/reservations/{id}"), updateBookingHandler);
        routeMap.put(new RouteKey("DELETE", "/reservations/waiter/{id}"), deleteReservationWaiterHandler);
        routeMap.put(new RouteKey("PATCH", "/reservations/waiter/{id}"), waiterUpdateHandler);
        routeMap.put(new RouteKey("GET", "/waiters/reservations"), getWaiterReservationHandler);
        routeMap.put(new RouteKey("POST", "/bookings/waiter"), waiterReservationHandler);
        routeMap.put(new RouteKey("POST", "/feedbacks"), feedbackCreationHandler);
        routeMap.put(new RouteKey("PUT", "/feedbacks"), feedbackModificationHandler);

        return routeMap;

    }
}
