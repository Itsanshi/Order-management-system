package com.restaurantback.dto;

import org.json.JSONObject;

import java.util.Objects;

public final class SignUp {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String password;

    public SignUp(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public static SignUp fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        String firstName = json.optString("firstName", null);
        String lastName = json.optString("lastName", null);
        String email = json.optString("email", null);
        String password = json.optString("password", null);

        return new SignUp(firstName, lastName, email, password);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SignUp signUp = (SignUp) o;
        return Objects.equals(firstName, signUp.firstName) && Objects.equals(lastName, signUp.lastName) && Objects.equals(email, signUp.email) && Objects.equals(password, signUp.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email, password);
    }

    @Override
    public String toString() {
        return "SignUp{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
