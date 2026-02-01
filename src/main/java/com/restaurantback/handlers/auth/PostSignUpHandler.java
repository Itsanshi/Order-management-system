package com.restaurantback.handlers.auth;

import com.amazonaws.services.cognitoidp.model.InvalidParameterException;
import com.amazonaws.services.cognitoidp.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidp.model.UsernameExistsException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.dto.SignUp;
import com.restaurantback.exceptions.authException.EmptyPasswordException;
import com.restaurantback.models.Role;
import com.restaurantback.models.User;
import com.restaurantback.services.CognitoSupport;
import com.restaurantback.services.UserService;
import com.restaurantback.utils.validator.SignUpValidator;
import org.json.JSONObject;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse;

import java.util.Optional;

public class PostSignUpHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final UserService userService;

    public PostSignUpHandler(CognitoIdentityProviderClient cognitoClient, UserService userService) {
        super(cognitoClient);
        this.userService = userService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            context.getLogger().log("sign up initialized");
            String body = apiGatewayProxyRequestEvent.getBody();

            if (body == null || body.isEmpty()) {
                throw new RuntimeException("Request body cannot be null or empty");
            }
            SignUp signUp = SignUp.fromJson(body);
            SignUpValidator.validateSignUp(signUp);

            Optional<String> userExists = userService.userWithEmailAlreadyExists(signUp.getEmail());
            String userRole = userExists.orElse("customer");

            AdminCreateUserResponse adminCreateUserResponse = cognitoSignUp(signUp, userRole, context);

            if (adminCreateUserResponse == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(409)
                        .withBody(new JSONObject().put("message", "User already registered.").toString());
            }

            AdminRespondToAuthChallengeResponse adminRespondToAuthChallengeResponse = confirmSignUp(signUp);

            String username = adminCreateUserResponse.user().username();
            String idToken = adminRespondToAuthChallengeResponse.authenticationResult().idToken();

            context.getLogger().log("new user created with username: " + username);
            context.getLogger().log(adminRespondToAuthChallengeResponse.toString());

            context.getLogger().log("role of new user will be: " + userRole);
            User newUser = new User(signUp.getFirstName(), signUp.getLastName(), "", signUp.getEmail(), Role.fromString(userRole), username);
            context.getLogger().log("user: " + newUser.toString());

            if (!userService.addUser(newUser)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(500)
                        .withBody(new JSONObject().put("message", "unable to save new user to amazon DynamoDB").toString());
            }

            context.getLogger().log("sign up completed.");

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(new JSONObject()
                            .put("message", "user registered successfully")
                            .put("idToken", idToken)
                            .toString());

        } catch (UsernameExistsException e) {
            context.getLogger().log(e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(409)
                    .withBody(new JSONObject().put("message", "A user with this email address already exists.").toString());
        } catch (InvalidPasswordException e) {
            context.getLogger().log(e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", "Invalid Password").toString());
        } catch (InvalidParameterException e) {
            context.getLogger().log(e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", "Invalid Parameters").toString());
        } catch (RuntimeException e) {

            context.getLogger().log(e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", e.getMessage()).toString());
        } catch (Exception e) {
            context.getLogger().log(e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(new JSONObject().put("message", "Registration Error").toString());
        }
    }
}
