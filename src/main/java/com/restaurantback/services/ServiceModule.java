package com.restaurantback.services;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.repository.*;
import com.restaurantback.utils.Base64ImageHandler;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class ServiceModule {

    @Singleton
    @Provides
    @Named("dynamoDbClient")
    DynamoDbClient dynamoDbClient(){
        return DynamoDbClient.create();
    }

    @Singleton
    @Provides
    @Named("dynamoDbEnhancedClient")
    DynamoDbEnhancedClient dynamoDbEnhancedClient(@Named("dynamoDbClient") DynamoDbClient dynamoDbClient){
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Singleton
    @Provides
    @Named("dishesService")
    DishesService dishesService(@Named("dynamoDbEnhancedClient") DynamoDbEnhancedClient dynamoDbEnhancedClient){
        return new DishesService(dynamoDbEnhancedClient);
    }

    @Singleton
    @Provides
    @Named("feedbackService")
    FeedbackService feedbackService(@Named("dynamoDbEnhancedClient") DynamoDbEnhancedClient dynamoDbEnhancedClient, @Named("amazonSQS")AmazonSQS amazonSQS){
        return new FeedbackService(dynamoDbEnhancedClient, amazonSQS);
    }

    @Singleton
    @Provides
    @Named("locationService")
    LocationService locationService(@Named("dynamoDbEnhancedClient") DynamoDbEnhancedClient dynamoDbEnhancedClient, @Named("dishesService") DishesService dishesService, @Named("locationRepository")LocationRepository locationRepository){
        return new LocationService(dynamoDbEnhancedClient, dishesService, locationRepository);
    }

    @Singleton
    @Provides
    @Named("tableService")
    TableService tableService(@Named("dynamoDbEnhancedClient") DynamoDbEnhancedClient dynamoDbEnhancedClient, @Named("locationService") LocationService locationService, @Named("tableRepository")TableRepository tableRepository){
        return new TableService(dynamoDbEnhancedClient, locationService, tableRepository);
    }

    @Singleton
    @Provides
    @Named("amazonDynamoDBClient")
    AmazonDynamoDB amazonDynamoDBClient() {
        return AmazonDynamoDBClientBuilder.standard().build();
    }

    @Singleton
    @Provides
    @Named("userService")
    UserService userService(@Named("amazonDynamoDBClient") AmazonDynamoDB amazonDynamoDB){
        return new UserService(amazonDynamoDB);
    }

    @Singleton
    @Provides
    @Named("profileService")
    ProfileService profileService(@Named("amazonDynamoDBClient") AmazonDynamoDB amazonDynamoDB, @Named("base64ImageHandler")Base64ImageHandler base64ImageHandler) {
        return new ProfileService(amazonDynamoDB, base64ImageHandler);
    }

    @Singleton
    @Provides
    @Named("bookingService")
    BookingService bookingService(@Named("bookingRepository") BookingRepository bookingRepository, @Named("tableService") TableService tableService, @Named("waiterService") WaiterService waiterService, @Named("locationService") LocationService locationService, @Named("employeeRepository")EmployeeRepository employeeRepository, @Named("dynamoDbEnhancedClient")DynamoDbEnhancedClient dynamoDbEnhancedClient){
        return new BookingService(bookingRepository, tableService, waiterService, locationService, employeeRepository, dynamoDbEnhancedClient);
    }

    @Singleton
    @Provides
    @Named("employeeService")
    EmployeeService employeeService(@Named("amazonDynamoDBClient") AmazonDynamoDB amazonDynamoDB, @Named("objectMapper")ObjectMapper objectMapper) {
        return new EmployeeService(amazonDynamoDB, objectMapper);
    }

    @Singleton
    @Provides
    @Named("waiterService")
    WaiterService waiterService(@Named("waiterRepository") WaiterRepository waiterRepository){
        return new WaiterService(waiterRepository);
    }

    @Singleton
    @Provides
    @Named("reservationService")
    ReservationService reservationService(@Named("bookingRepository")BookingRepository bookingRepository,
                                          @Named("employeeRepository") EmployeeRepository employeeRepository,
                                          @Named("tableRepository") TableRepository tableRepository,
                                          @Named("waiterRepository") WaiterRepository waiterRepository,
                                          @Named("locationService") LocationService locationService,
                                          @Named("userRepository") UserRepository userRepository){
        return new ReservationService(bookingRepository, employeeRepository, waiterRepository, tableRepository, locationService, userRepository);
    }

    @Singleton
    @Provides
    @Named("salesReportsService")
    SalesReportsService salesReportsService(@Named("dynamoDbEnhancedClient") DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        return new SalesReportsService(dynamoDbEnhancedClient);
    }

    @Singleton
    @Provides
    @Named("staffReportsService")
    StaffReportsService staffReportsService(@Named("dynamoDbEnhancedClient") DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        return new StaffReportsService(dynamoDbEnhancedClient);
    }

    @Singleton
    @Provides
    @Named("excelService")
    ExcelService excelService() {
        return new ExcelService();
    }

    @Singleton
    @Provides
    @Named("emailService")
    EmailService emailService(@Named("amazonSESClient")AmazonSimpleEmailService sesClient) {
        return new EmailService(sesClient);
    }

}
