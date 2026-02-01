package com.restaurantback.dto;

import com.restaurantback.utils.validator.EmailValidator;
import com.restaurantback.utils.validator.PasswordValidator;
import org.json.JSONObject;

import java.util.Objects;

public final class SignIn {

    private final String email;
    private final String password;

    public SignIn(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public static SignIn fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        String email = json.optString("email", null);
        String password = json.optString("password", null);
        return new SignIn(email,password);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SignIn signIn = (SignIn) o;
        return Objects.equals(email, signIn.email) && Objects.equals(password, signIn.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, password);
    }

    @Override
    public String toString() {
        return "SignIn{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
