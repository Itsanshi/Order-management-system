package com.restaurantback.utils.validator;

import com.restaurantback.dto.SignUp;

public class SignUpValidator {

    public static void validateSignUp(SignUp signUp) {
        if (signUp.getFirstName() == null || signUp.getFirstName().trim().isEmpty()) {
            if (signUp.getFirstName() == null) {
                throw new RuntimeException("First name is required.");
            } else {
                throw new RuntimeException("First name cannot be empty.");
            }
        }

        if (signUp.getLastName() == null || signUp.getLastName().trim().isEmpty()) {
            if (signUp.getLastName() == null) {
                throw new RuntimeException("Last name is required.");
            } else {
                throw new RuntimeException("Last name cannot be empty.");
            }
        }

        if (signUp.getEmail() == null || signUp.getEmail().trim().isEmpty()) {
            if (signUp.getEmail() == null) {
                throw new RuntimeException("Email is required.");
            } else {
                throw new RuntimeException("Email cannot be empty.");
            }
        }

        if (!EmailValidator.validateEmail(signUp.getEmail())) {
            throw new RuntimeException("Invalid email format.");
        }

        if (signUp.getPassword() == null || signUp.getPassword().trim().isEmpty()) {
            if (signUp.getPassword() == null) {
                throw new RuntimeException("Password is required.");
            } else {
                throw new RuntimeException("Password cannot be empty.");
            }
        }

        PasswordValidator.validatePassword(signUp.getPassword());
    }


}
