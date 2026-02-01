package com.restaurantback.utils.validator;

import com.restaurantback.dto.profile.ChangePassword;

public class ChangePasswordDtoValidator {

    public static void validate(ChangePassword changePassword) {

        if (changePassword.getOldPassword() == null) {
            throw new RuntimeException("Old Password is required");
        }

        if (changePassword.getOldPassword().isEmpty()) {
            throw new RuntimeException("Old Password cannot be empty");
        }

        if (changePassword.getNewPassword() == null) {
            throw new RuntimeException("New Password is required");
        }

        if (changePassword.getNewPassword().isEmpty()) {
            throw new RuntimeException("New Password cannot be empty");
        }

        if (changePassword.getOldPassword().equals(changePassword.getNewPassword())) {
            throw new RuntimeException("Old password and new password cannot be same.");
        }

        PasswordValidator.validatePassword(changePassword.getNewPassword());
    }
}
