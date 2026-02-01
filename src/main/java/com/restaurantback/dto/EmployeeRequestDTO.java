package com.restaurantback.dto;

public class EmployeeRequestDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String locationId;
    private String role;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocation_id(String locationId) {
        this.locationId = locationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
