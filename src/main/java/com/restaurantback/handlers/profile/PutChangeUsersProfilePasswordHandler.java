package com.restaurantback.handlers.profile;

import com.amazonaws.services.cognitoidp.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.profile.ChangePassword;
import com.restaurantback.services.CognitoSupport;
import com.restaurantback.utils.GetAccessTokenFromRequest;
import com.restaurantback.utils.GetDataFromJwt;
import com.restaurantback.utils.validator.ChangePasswordDtoValidator;
import org.json.JSONObject;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

public class PutChangeUsersProfilePasswordHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;

    public PutChangeUsersProfilePasswordHandler(CognitoIdentityProviderClient cognitoClient, ObjectMapper objectMapper) {
        super(cognitoClient);
        this.objectMapper = objectMapper;
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            context.getLogger().log(request.getHttpMethod() + " " + request.getResource());

            ChangePassword changePassword = objectMapper.readValue(request.getBody(), ChangePassword.class);
            ChangePasswordDtoValidator.validate(changePassword);

            String idToken = GetAccessTokenFromRequest.getIdToken(request);
            context.getLogger().log("Id token: " + idToken);

            String email = GetDataFromJwt.extractDataFromToken(idToken).getEmail();

            String accessToken = cognitoSignIn(email, changePassword.getOldPassword())
                    .authenticationResult()
                    .accessToken();

            context.getLogger().log("access token: " + accessToken);

            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("access token is empty");
            }

            updatePassword(accessToken, changePassword);

            context.getLogger().log("password updated");
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(new JSONObject().put("message", "Password has been successfully updated").toString());

        } catch (NotAuthorizedException e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", "User not authorized").toString());
        } catch (UserNotFoundException e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", "User not found").toString());
        } catch (InvalidPasswordException e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", "Invalid Password Exception").toString());
        } catch (InvalidParameterException e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", "Invalid Parameter Exception").toString());
        } catch (CognitoIdentityProviderException e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", "Invalid username or password").toString());
        } catch (RuntimeException e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", e.getMessage()).toString());
        } catch (Exception e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(new JSONObject().put("message", "Internal Server Error").toString());
        }
    }
}
