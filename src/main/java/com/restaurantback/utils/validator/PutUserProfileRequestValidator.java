package com.restaurantback.utils.validator;

import com.restaurantback.dto.profile.PutUsersProfileRequest;

public class PutUserProfileRequestValidator {

    public static void validate(PutUsersProfileRequest putUsersProfileRequest) {

        if (putUsersProfileRequest.getFirstName() == null) {
            throw new RuntimeException("First name is required.");
        }

        if (putUsersProfileRequest.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("First name cannot be empty.");
        }

        if (putUsersProfileRequest.getLastName() == null) {
            throw new RuntimeException("Last name is required.");
        }

        if (putUsersProfileRequest.getLastName().trim().isEmpty()) {
            throw new RuntimeException("Last name cannot be empty.");
        }

        if (putUsersProfileRequest.getBase64encodedImage() == null) {
            throw new RuntimeException("Base64 encoded image cannot be null.");
        }

        if (!isValidBase64(putUsersProfileRequest.getBase64encodedImage())) {
            throw new RuntimeException("Base64 encoded image is invalid.");
        }
    }

    private static boolean isValidBase64(String base64String) {
        try {
            java.util.Base64.getDecoder().decode(base64String);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
