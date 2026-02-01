package com.restaurantback.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DishDTO {
    private String id;
    private String name;
    private String price;
    private String weight;
    private String imageUrl;
    private String description;
    private String calories;
    private String carbs;
    private String fats;
    private String proteins;
    private String vitamins;
    private String dishType;
    private Boolean isAvailable;
    private Boolean isPopular;
    private String state;
    private String popularityScore;

    public DishDTO(String name, String price, String weight, String image) {
        this.name=name;
        this.price=price;
        this.weight=weight;
        this.imageUrl=image;
    }

    public Boolean getIsPopular() {
        return isPopular;
    }

    public String getPopularityScore() {
        return popularityScore;
    }
}
