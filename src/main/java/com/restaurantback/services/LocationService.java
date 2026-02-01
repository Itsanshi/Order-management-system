package com.restaurantback.services;


import com.restaurantback.dto.DishInfoDTO;
import com.restaurantback.dto.LocationSmallDTO;
import com.restaurantback.exceptions.reservationException.NotFoundException;
import com.restaurantback.models.Dish;
import com.restaurantback.models.Location;
import com.restaurantback.repository.LocationRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.List;

public class LocationService {

    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;
    private final DishesService dishesService;
    private final LocationRepository locationRepository;

    public LocationService(DynamoDbEnhancedClient dynamoDbEnhancedClient, DishesService dishesService, LocationRepository locationRepository) {
        this.enhancedClient = dynamoDbEnhancedClient;
        this.tableName = System.getenv("locationTable");
        this.dishesService = dishesService;
        this.locationRepository = locationRepository;
    }

    public List<LocationSmallDTO> getAllLocations(){
        DynamoDbTable<Location> table = enhancedClient.table(tableName, TableSchema.fromBean(Location.class));
        List<Location> locations = new ArrayList<>();
        table.scan().items().forEach(locations::add);
        List<LocationSmallDTO> locationSmallDTOS = new ArrayList<>();
        for(Location location : locations){
            locationSmallDTOS.add(new LocationSmallDTO(location.getId(), location.getAddress()));
        }
        return locationSmallDTOS;
    }

    public Location getLocationById(String locationId){
        DynamoDbTable<Location> locationTable = enhancedClient.table(tableName, TableSchema.fromBean(Location.class));
        Key key = Key.builder()
                .partitionValue(locationId)
                .build();

        return locationTable.getItem(r -> r.key(key));
    }

    public String getLocationAddressByLocationId(String locationId){
        Location location = getLocationById(locationId);
        return location.getAddress();
    }

    public List<Location> getAllLocationsList(){
        DynamoDbTable<Location> table = enhancedClient.table(tableName, TableSchema.fromBean(Location.class));
        List<Location> locations = new ArrayList<>();
        table.scan().items().forEach(locations::add);
        return locations;
    }


    public DishInfoDTO getSpecialityDishesByLocationId(String locationId) {
        List<Location> locations = getAllLocationsList();
        String dishName =  locations.stream()
                .filter(location -> location.getId().equalsIgnoreCase(locationId))
                .map(Location::getSpecialityDishes)
                .findFirst()
                .orElse(null);
        List<Dish> dishes = dishesService.getListOfAllDishes();
        DishInfoDTO dishInfoDTO = null;
        for(Dish dish : dishes){
            if(dish.getName().equalsIgnoreCase(dishName)){
                dishInfoDTO = new DishInfoDTO(dish.getName(), dish.getPrice(), dish.getWeight(), dish.getImage());
            }
        }

        return dishInfoDTO;

    }

    public String getLocationAddressById(String locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("Location ID cannot be null");
        }

        String address = locationRepository.getLocationAddress(locationId);
        if (address == null) {
            throw new NotFoundException(String.format("Location with id: %s does not exist", locationId));
        }

        return address;
    }

    public boolean doesLocationExist(String locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("Location ID cannot be null");
        }

        return locationRepository.doesLocationExist(locationId);
    }



}
