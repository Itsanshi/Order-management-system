package com.restaurantback.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DishResponseDTO {
    private String id;
    private String name;
    private String imageUrl;
    private String price;
    private String weight;
    private String state;
}
