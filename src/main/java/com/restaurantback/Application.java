package com.restaurantback;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.dto.RouteKey;
import com.restaurantback.handlers.HandlersModule;
import com.restaurantback.repository.RepositoryModule;
import com.restaurantback.services.ServiceModule;
import com.restaurantback.utils.UtilsModule;
import dagger.Component;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

@Singleton

@Component(modules = {UtilsModule.class, ServiceModule.class, HandlersModule.class, RepositoryModule.class})

public interface Application {

    @Named("cors")
    Map<String, String> getCorsHeaders();

    @Named("cognitoClient")
    CognitoIdentityProviderClient getCognitoClient();

    @Named("routeMap")
    Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> getHandlersByRouteKey();

}

