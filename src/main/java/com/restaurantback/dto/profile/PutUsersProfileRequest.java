package com.restaurantback.dto.profile;

public class PutUsersProfileRequest {

    private String base64encodedImage;
    private String firstName;
    private String lastName;

    public String getBase64encodedImage() {
        return base64encodedImage;
    }

    public void setBase64encodedImage(String base64encodedImage) {
        this.base64encodedImage = base64encodedImage;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
