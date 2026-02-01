package com.restaurantback.dto;

public class CognitoToken {

    private String family_name;
    private String given_name;
    private String email;
    private String sub;

    public CognitoToken(String family_name, String given_name, String email, String sub, String role) {
        this.family_name = family_name;
        this.given_name = given_name;
        this.email = email;
        this.sub = sub;
        this.role = role;
    }

    private String role;

    public String getFamily_name() {
        return family_name;
    }

    public void setFamily_name(String family_name) {
        this.family_name = family_name;
    }

    public String getGiven_name() {
        return given_name;
    }

    public void setGiven_name(String given_name) {
        this.given_name = given_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
