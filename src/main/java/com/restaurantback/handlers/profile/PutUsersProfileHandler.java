package com.restaurantback.handlers.profile;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantback.dto.profile.PutUsersProfileRequest;
import com.restaurantback.services.ProfileService;
import com.restaurantback.utils.GetAccessTokenFromRequest;
import com.restaurantback.utils.validator.PutUserProfileRequestValidator;
import org.json.JSONObject;

public class PutUsersProfileHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    public PutUsersProfileHandler(ProfileService profileService, ObjectMapper objectMapper) {
        this.profileService = profileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            context.getLogger().log(request.getHttpMethod() + " " + request.getResource());
            PutUsersProfileRequest putUsersProfileRequest = objectMapper.readValue(request.getBody(), PutUsersProfileRequest.class);

            profileService.updateUserProfile(request, putUsersProfileRequest);
            PutUserProfileRequestValidator.validate(putUsersProfileRequest);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(new JSONObject().put("message", "Profile has been successfully updated").toString());

        } catch (RuntimeException e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", e.getMessage()).toString());
        }
        catch (Exception e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(new JSONObject().put("message", "Internal Server Error").toString());
        }

    }
}
