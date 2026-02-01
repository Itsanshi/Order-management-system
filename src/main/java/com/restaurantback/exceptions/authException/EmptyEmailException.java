package com.restaurantback.exceptions.authException;

public class EmptyEmailException extends Exception{

    public EmptyEmailException(String message) {
        super(message);
    }
}
