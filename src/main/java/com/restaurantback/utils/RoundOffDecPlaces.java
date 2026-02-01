package com.restaurantback.utils;

public class RoundOffDecPlaces {
    public static float roundOffTo2DecPlaces(float val) {
        String res = String.format("%.2f", val);
        return Float.parseFloat(res);
    }
}
