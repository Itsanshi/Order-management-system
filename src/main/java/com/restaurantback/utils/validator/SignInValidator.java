package com.restaurantback.utils.validator;

import com.restaurantback.dto.SignIn;
import com.restaurantback.exceptions.authException.EmptyEmailException;
import com.restaurantback.exceptions.authException.EmptyPasswordException;
import com.restaurantback.exceptions.authException.InvalidEmailException;

public class SignInValidator {
    
    public static void validateSingIn(SignIn signIn) throws EmptyEmailException, EmptyPasswordException, InvalidEmailException {

        if (signIn.getEmail() == null || signIn.getEmail().trim().isEmpty()) {
            if (signIn.getEmail() == null) {
                throw new EmptyEmailException("Email is required.");
            } else {
                throw new EmptyEmailException("Email cannot be empty.");
            }
        }

        if (!EmailValidator.validateEmail(signIn.getEmail())) {
            throw new InvalidEmailException("Invalid email format.");
        }

        if (signIn.getPassword() == null || signIn.getPassword().trim().isEmpty()) {
            if (signIn.getPassword() == null) {
                throw new EmptyPasswordException("Password is required.");
            } else {
                throw new EmptyPasswordException("Password cannot be empty.");
            }
        }

        PasswordValidator.validatePassword(signIn.getPassword());
    }
}
