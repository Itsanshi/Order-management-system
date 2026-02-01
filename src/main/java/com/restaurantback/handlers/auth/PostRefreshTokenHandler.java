package com.restaurantback.handlers.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.services.CognitoSupport;
import org.json.JSONObject;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;


public class PostRefreshTokenHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public PostRefreshTokenHandler(CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            context.getLogger().log("Refresh token request initialized");

            // Extract refresh token from the request body
            JSONObject requestBody = new JSONObject(requestEvent.getBody());
            String refreshToken = requestBody.getString("refreshToken");

            AuthenticationResultType authResult = getAuthenticationTokenWithRefreshToken(refreshToken);

            String newAccessToken = authResult.accessToken();
            String newIdToken = authResult.idToken();
            String newRefreshToken = authResult.refreshToken();

            context.getLogger().log("Tokens refreshed successfully. access token: " + newAccessToken);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(new JSONObject()
                            .put("accessToken", newAccessToken)
                            .put("idToken", newIdToken)
                            .put("refreshToken", newRefreshToken)
                            .toString());

        } catch (CognitoIdentityProviderException e) {
            context.getLogger().log(e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", "Invalid refresh token").toString());
        } catch (Exception e) {
            context.getLogger().log(e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(new JSONObject().put("message", "Error refreshing tokens").toString());
        }
    }
}
