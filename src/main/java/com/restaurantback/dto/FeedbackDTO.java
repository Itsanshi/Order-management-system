package com.restaurantback.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FeedbackDTO {

    private String id;
    private String rate;
    private String comment;
    private String userName;
    private String date;
    private String type;
    private String locationId;
    private String userAvatarUrl;

}
