package com.restaurantback.handlers.auth;

import com.amazonaws.services.cognitoidp.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.dto.CognitoToken;
import com.restaurantback.dto.SignIn;
import com.restaurantback.exceptions.authException.EmptyEmailException;
import com.restaurantback.exceptions.authException.EmptyPasswordException;
import com.restaurantback.exceptions.authException.InvalidEmailException;
import com.restaurantback.services.CognitoSupport;
import com.restaurantback.utils.GetDataFromJwt;
import com.restaurantback.utils.validator.SignInValidator;
import org.json.JSONObject;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import java.util.*;
public class PostSignInHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public PostSignInHandler(CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            context.getLogger().log("sign in request initialized");

            String body = apiGatewayProxyRequestEvent.getBody();
            if (body == null || body.isEmpty()) {
                throw new RuntimeException("Request Body cannot be null or empty");
            }

            SignIn signIn = SignIn.fromJson(body);
            SignInValidator.validateSingIn(signIn);

            AuthenticationResultType authenticationResult = cognitoSignIn(signIn.getEmail(), signIn.getPassword())
                    .authenticationResult();

            String idToken = authenticationResult.idToken();
            String accessToken = authenticationResult.accessToken();
            String refreshToken = authenticationResult.refreshToken();

            CognitoToken cognitoToken = GetDataFromJwt.extractDataFromToken(idToken);

            context.getLogger().log("sign in successfully.\n" + "idToken: " + idToken + "\naccessToken: " + accessToken);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of(
                            "X-Id-Token", idToken,
                            "X-Refresh-Token", refreshToken,
                            "X-Access-Token", accessToken
                    )).withBody(new JSONObject()
                            .put("idToken", idToken)
                            .put("accessToken", accessToken)
                            .put("refreshToken", refreshToken)
                            .put("username", cognitoToken.getGiven_name() + " " + cognitoToken.getFamily_name())
                            .put("role", cognitoToken.getRole())
                            .put("email", cognitoToken.getEmail())
                            .toString());

        } catch (UserNotFoundException e) {
            context.getLogger().log(e.toString());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", "user not found").toString());
        } catch (InvalidPasswordException e) {
            context.getLogger().log(e.toString());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", "Incorrect password").toString());
        } catch (CognitoIdentityProviderException e) {
            context.getLogger().log(e.toString());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", "Incorrect username or password").toString());
        } catch (EmptyEmailException | EmptyPasswordException | InvalidEmailException e) {
            context.getLogger().log(e.toString());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", e.getMessage()).toString());
        } catch (RuntimeException e) {
            context.getLogger().log(e.toString());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(new JSONObject().put("message", e.getMessage()).toString());
        } catch (Exception e) {
            context.getLogger().log(e.toString());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", "There was an error in the request.").toString());
        }
    }
}
