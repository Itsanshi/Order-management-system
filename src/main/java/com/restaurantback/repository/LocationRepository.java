package com.restaurantback.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;

import javax.inject.Inject;
import java.util.Map;

public class LocationRepository {
    private final String tableName = System.getenv("locationTable");
    AmazonDynamoDB dynamoDB;

    @Inject
    public LocationRepository(AmazonDynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    public Map<String, AttributeValue> getLocationDataById(String locationId) {
        GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of(
                        "location_id", new AttributeValue().withS(locationId)
                ));

        return dynamoDB.getItem(getItemRequest).getItem();
    }

    public String getLocationAddress(String locationId) {
        Map<String, AttributeValue> locationData = getLocationDataById(locationId);
        if (locationData != null && locationData.containsKey("address")) {
            return locationData.get("address").getS();
        }
        return null;
    }


    public boolean doesLocationExistsWithIdAndLocationId(String locationId){
        GetItemRequest getItemRequest = new GetItemRequest(tableName,Map.of(
                "location_id",new AttributeValue(locationId))
        ).withProjectionExpression("location_id");
        return dynamoDB.getItem(getItemRequest).getItem()!=null;
    }

    public boolean doesLocationExist(String locationId) {
        return getLocationDataById(locationId) != null;
    }

}