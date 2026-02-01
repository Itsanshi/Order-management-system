package com.restaurantback.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DishSmallDTO {
    private String id;
    private String name;
    private String imageUrl;
    private String price;
    private String weight;
    private String state;
    private String dishType;
    private Boolean isPopular;
    private String popularityScore;
}
