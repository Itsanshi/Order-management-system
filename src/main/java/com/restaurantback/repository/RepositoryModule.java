package com.restaurantback.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class RepositoryModule {

    @Singleton
    @Provides
    @Named("bookingRepository")
    BookingRepository bookingRepository(@Named("amazonDynamoDBClient")AmazonDynamoDB amazonDynamoDB, @Named("dynamoDbEnhancedClient")DynamoDbEnhancedClient dynamoDbEnhancedClient){
        return new BookingRepository(amazonDynamoDB, dynamoDbEnhancedClient);
    }

    @Singleton
    @Provides
    @Named("locationRepository")
    LocationRepository locationRepository(@Named("amazonDynamoDBClient")AmazonDynamoDB amazonDynamoDB){
        return new LocationRepository(amazonDynamoDB);
    }

    @Singleton
    @Provides
    @Named("tableRepository")
    TableRepository tableRepository(@Named("amazonDynamoDBClient")AmazonDynamoDB amazonDynamoDB, @Named("dynamoDbEnhancedClient")DynamoDbEnhancedClient dynamoDbEnhancedClient){
        return new TableRepository(amazonDynamoDB, dynamoDbEnhancedClient);
    }

    @Singleton
    @Provides
    @Named("waiterRepository")
    WaiterRepository waiterRepository(@Named("amazonDynamoDBClient")AmazonDynamoDB amazonDynamoDB, @Named("dynamoDbEnhancedClient")DynamoDbEnhancedClient dynamoDbEnhancedClient){
        return new WaiterRepository(amazonDynamoDB, dynamoDbEnhancedClient);
    }

    @Singleton
    @Provides
    @Named("employeeRepository")
    EmployeeRepository employeeRepository(@Named("dynamoDbEnhancedClient")DynamoDbEnhancedClient dynamoDbEnhancedClient){
        return new EmployeeRepository(dynamoDbEnhancedClient);
    }

    @Singleton
    @Provides
    @Named("userRepository")
    UserRepository userRepository(@Named("dynamoDbEnhancedClient")DynamoDbEnhancedClient dynamoDbEnhancedClient){
        return new UserRepository(dynamoDbEnhancedClient);
    }
}