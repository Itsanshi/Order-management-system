package com.restaurantback.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DishInfoDTO {

    private String name;
    private String price;
    private String weight;
    private String imageUrl;
}
