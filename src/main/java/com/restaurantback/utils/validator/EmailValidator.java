package com.restaurantback.utils.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailValidator {
    private static final String REGEX = "^(?!\\d+$)[A-Za-z\\d+_.-]+@[A-Za-z.-]+\\.[A-Za-z]{2,6}$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    public static boolean validateEmail(String email) {
        if (email == null) {
            return false;
        }
        Matcher matcher = PATTERN.matcher(email);
        return matcher.matches();
    }
}
