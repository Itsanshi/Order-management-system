package com.restaurantback.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.JSONObject;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@NoArgsConstructor
@Data
public class User {

    private String cognitoSub;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private String email;
    private Role role;


    public User(String firstName, String lastName, String imageUrl, String email, Role role, String cognitoSub) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.imageUrl = imageUrl;
        this.email = email;
        this.role = role;
        this.cognitoSub = cognitoSub;
    }

    public static User fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);

        String firstName = json.optString("firstName", null);
        String cognitoSub = json.optString("cognitoSub", null);
        String lastName = json.optString("firstName", null);
        String imageUrl = json.optString("firstName", null);
        String email = json.optString("firstName", null);
        Role role = Role.valueOf(json.optString("role", null));

        return new User(firstName, lastName, imageUrl, email, role, cognitoSub);
    }


    public void setCognitoSub(String cognitoSub) {
        this.cognitoSub = cognitoSub;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }


    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }


    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "cognitoSub='" + cognitoSub + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}

