package com.restaurantback;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.dto.RouteKey;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.environment.ValueTransformer;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
        lambdaName = "api_handler",
        roleName = "api_handler-role",
        isPublishVersion = true,
        runtime = DeploymentRuntime.JAVA21,
        aliasName = "${lambdas_alias_name}",
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@DependsOn(resourceType = ResourceType.COGNITO_USER_POOL, name = "${restaurant_userpool}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${user_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${dish_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${feedback_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${location_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${tables_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${timeslot_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${reservation_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${booking_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${cart_table}")
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${waiter_table}")

@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "REGION", value = "${region}"),
        @EnvironmentVariable(key = "COGNITO_ID", value = "${restaurant_userpool}", valueTransformer = ValueTransformer.USER_POOL_NAME_TO_USER_POOL_ID),
        @EnvironmentVariable(key = "CLIENT_ID", value = "${restaurant_userpool}", valueTransformer = ValueTransformer.USER_POOL_NAME_TO_CLIENT_ID),
        @EnvironmentVariable(key = "userTable", value = "${user_table}"),
        @EnvironmentVariable(key = "dishTable", value = "${dish_table}"),
        @EnvironmentVariable(key = "feedbackTable", value = "${feedback_table}"),
        @EnvironmentVariable(key = "locationTable", value = "${location_table}"),
        @EnvironmentVariable(key = "tablesTable", value = "${tables_table}"),
        @EnvironmentVariable(key = "timeslotTable", value = "${timeslot_table}"),
        @EnvironmentVariable(key = "reservationTable", value = "${reservation_table}"),
        @EnvironmentVariable(key = "bookingTable", value = "${booking_table}"),
        @EnvironmentVariable(key = "cartTable", value = "${cart_table}"),
        @EnvironmentVariable(key = "employeeTable", value = "${employee_table}"),
        @EnvironmentVariable(key = "waiterTable", value = "${waiter_table}"),
        @EnvironmentVariable(key = "profileImageBucket", value = "${profile_image_bucket}"),
        @EnvironmentVariable(key = "reportQueue", value = "${report_sqs_queue}"),
})

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Application daggerContext = DaggerApplication.create();
    private final Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> handlersByRouteKey = daggerContext.getHandlersByRouteKey();
    private final Map<String, String> headersForCORS = daggerContext.getCorsHeaders();

    public ApiHandler() {
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        APIGatewayProxyResponseEvent response = getHandler(request)
                .handleRequest(request, context);

        if (response.getHeaders() == null) {
            return response.withHeaders(headersForCORS);
        }

        Map<String, String> mergedHeaders = new HashMap<>(response.getHeaders());
        mergedHeaders.putAll(headersForCORS);

        return response.withHeaders(mergedHeaders);
    }

    /**
     * Get RouteKey based on HTTP method and path
     */
    private RouteKey getRouteKey(APIGatewayProxyRequestEvent requestEvent) {
        return new RouteKey(requestEvent.getHttpMethod(), requestEvent.getPath());
    }

//    some changes need to be done

    private RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getHandler(APIGatewayProxyRequestEvent requestEvent) {
//        return handlersByRouteKey.get(getRouteKey(requestEvent));
        String incomingMethod = requestEvent.getHttpMethod();
        String incomingPath = requestEvent.getPath();

        System.out.println(incomingMethod + incomingPath);

        for (RouteKey routeKey : handlersByRouteKey.keySet()) {
            if (routeKey.matches(incomingMethod, incomingPath)) {
                Map<String, String> pathVariables = routeKey.extractPathVariables(incomingPath);
                requestEvent.setPathParameters(pathVariables);
                RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> f = handlersByRouteKey.get(routeKey);
                System.out.println(f);
                return f;
            }
        }
        return handlersByRouteKey.get(getRouteKey(requestEvent));
    }
}