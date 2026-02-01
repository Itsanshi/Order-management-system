package com.restaurantback;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.repository.BookingRepository;
import com.restaurantback.repository.EmployeeRepository;
import com.restaurantback.repository.RepositoryModule;
import com.restaurantback.repository.WaiterRepository;
import com.restaurantback.services.*;
import com.restaurantback.utils.UtilsModule;
import dagger.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Component(modules = {
        ServiceModule.class,
        UtilsModule.class,
        RepositoryModule.class
})

public interface SQSApplication {

    @Named("amazonDynamoDBClient")
    AmazonDynamoDB getAmazonDynamoDbClient();

    @Named("amazonSQS")
    AmazonSQS getAmazonSqsClient();

    @Named("bookingService")
    BookingService getBookingService();

    @Named("bookingRepository")
    BookingRepository getBookingRepository();

    @Named("objectMapper")
    ObjectMapper getObjectMapper();

    @Named("dynamoDbEnhancedClient")
    DynamoDbEnhancedClient getDyaDynamoDbEnhancedClient();

    @Named("waiterRepository")
    WaiterRepository getWaiterRepository();

    @Named("employeeRepository")
    EmployeeRepository getEmployeeRepository();

    @Named("feedbackService")
    FeedbackService getFeedbackService();

    @Named("salesReportsService")
    SalesReportsService getSalesReportsService();

    @Named("staffReportsService")
    StaffReportsService getStaffReportsService();

    @Named("excelService")
    ExcelService getExcelService();

    @Named("emailService")
    EmailService getEmailService();

}
