package com.restaurantback.exceptions.AuthorizationException;

public class NotAuthorizedException extends RuntimeException{

    public NotAuthorizedException(String message) {
        super(message);
    }
}
