package com.restaurantback.handlers.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.services.CognitoSupport;
import com.restaurantback.utils.GetAccessTokenFromRequest;
import org.json.JSONObject;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

public class PostLogOutHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public PostLogOutHandler(CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            context.getLogger().log("Logout request initialized");

            // Extract access token from the Authorization header
            String idToken = GetAccessTokenFromRequest.getIdToken(request);
            context.getLogger().log("got id token from header. " + idToken);

            // Extract access token from the request body
            JSONObject requestBody = new JSONObject(request.getBody());
            String accessToken = requestBody.getString("accessToken");
            context.getLogger().log("got id token from header. " + accessToken);

            // Use the GlobalSignOut API to revoke tokens
            var res = cognitoSignOut(accessToken);

            context.getLogger().log("User logged out successfully " + res.toString());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(new JSONObject().put("message", "User logged out successfully").toString());

        } catch (CognitoIdentityProviderException e) {
            context.getLogger().log(e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", "Invalid access token").toString());
        } catch (RuntimeException e) {
            context.getLogger().log(e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", e.getMessage()).toString());
        } catch (Exception e) {
            context.getLogger().log(e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(new JSONObject().put("message", "Error logging out").toString());
        }
    }
}
