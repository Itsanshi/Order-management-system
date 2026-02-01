package com.restaurantback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableAvailableDTO {
    private String locationId;
    private String locationAddress;
    private String tableNumber;
    private int capacity;
    private List<String> availableSlots;
    private String imageUrl;
}

