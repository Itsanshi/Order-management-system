package com.restaurantback.utils;

import com.restaurantback.dto.CognitoToken;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

public class GetDataFromJwt {
    public static CognitoToken extractDataFromToken(String jwtToken) {
        if (jwtToken.contains("Bearer")) {
            jwtToken = jwtToken.replace("Bearer ", "");
        }

        DecodedJWT decodedJWT = JWT.decode(jwtToken);

        CognitoToken cognitoToken=
                new CognitoToken(decodedJWT.getClaim("family_name").asString(),
                        decodedJWT.getClaim("given_name").asString(),
                        decodedJWT.getClaim("email").asString(),
                        decodedJWT.getClaim("sub").asString(),
                        decodedJWT.getClaim("custom:role").asString());

        return cognitoToken;
    }
}
