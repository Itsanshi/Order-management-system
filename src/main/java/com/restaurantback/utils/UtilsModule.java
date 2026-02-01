package com.restaurantback.utils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import software.amazon.awssdk.regions.Region;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

@Module
public class UtilsModule {

    @Singleton
    @Provides
    @Named("cors")
    Map<String, String> provideCorsHeaders() {
        return Map.of(
                "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token, X-Access-Token",
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Methods", "*",
                "Accept-Version", "*"
        );
    }

    @Singleton
    @Provides
    @Named("objectMapper")
    ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Singleton
    @Provides
    @Named("cognitoClient")
    CognitoIdentityProviderClient provideCognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(System.getenv("REGION")))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

    }

    @Singleton
    @Provides
    @Named("amazonS3")
    AmazonS3 provideAmazons3() {
        return AmazonS3ClientBuilder.defaultClient();
    }

    @Singleton
    @Provides
    @Named("amazonSQS")
    AmazonSQS providAmazonSQS() {
        return AmazonSQSClientBuilder.defaultClient();
    }

    @Singleton
    @Provides
    @Named("amazonSESClient")
    AmazonSimpleEmailService provideAmazonSimpleEmailService() {
        return AmazonSimpleEmailServiceClientBuilder.standard().build();
    }

    @Singleton
    @Provides
    @Named("base64ImageHandler")
    Base64ImageHandler provideBase64ImageHandler(@Named("amazonS3") AmazonS3 amazonS3) {
        return new Base64ImageHandler(amazonS3);
    }
}
