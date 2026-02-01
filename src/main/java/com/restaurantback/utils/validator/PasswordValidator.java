package com.restaurantback.utils.validator;

public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 16;

    public static void validatePassword(String password) {

        if (password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters.");
        }

        if (password.length() > 16) {
            throw new RuntimeException("Password must be at most 16 characters.");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Password must contain lowercase letters.");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Password must contain uppercase letters.");
        }

        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException("Password must contain at least one number.");
        }

        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw new RuntimeException("Password must contain special characters.");
        }

    }

}
