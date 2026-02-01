package com.restaurantback.exceptions.authException;

public class EmptyPasswordException extends Exception{

    public EmptyPasswordException(String message) {
        super(message);
    }
}
