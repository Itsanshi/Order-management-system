package com.restaurantback.dto.profile;

import org.json.JSONObject;

public class ProfileResponse {

    private String firstName;
    private String imageUrl;
    private String lastName;

    public ProfileResponse(String firstName, String imageUrl, String lastName) {
        this.firstName = firstName;
        this.imageUrl = imageUrl;
        this.lastName = lastName;
    }

    public static ProfileResponse fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        String firstName = json.optString("firstName", null);
        String imageUrl = json.optString("imageUrl", null);
        String lastName = json.optString("lastName", null);

        return new ProfileResponse(firstName, imageUrl, lastName);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return "ProfileResponse{" +
                "firstName='" + firstName + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
