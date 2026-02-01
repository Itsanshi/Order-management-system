package com.restaurantback.exceptions.reservationException;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
