package com.restaurantback.services;

import com.restaurantback.dto.DishDTO;
import com.restaurantback.dto.DishSmallDTO;
import com.restaurantback.exceptions.dishException.DishNotFoundException;
import com.restaurantback.models.Dish;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DishesService {

    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;

    public DishesService(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.enhancedClient = dynamoDbEnhancedClient;
        this.tableName = System.getenv("dishTable");
    }

    public List<Dish> getPopularDishes(){
        DynamoDbTable<Dish> table = enhancedClient.table(tableName, TableSchema.fromBean(Dish.class));
        List<Dish> dishes = new ArrayList<>();
        table.scan().items().forEach(dishes::add);
        List<Dish> popularDishes = new ArrayList<>();
        System.out.println(dishes);
        for(Dish dish : dishes){
            if(dish.isPopular()){
                popularDishes.add(dish);
            }
        }
        System.out.println(popularDishes);
        return popularDishes;
    }

    public List<Dish> getListOfAllDishes(){
        DynamoDbTable<Dish> table = enhancedClient.table(tableName, TableSchema.fromBean(Dish.class));
        List<Dish> dishes = new ArrayList<>();
        table.scan().items().forEach(dishes::add);
        return dishes;
    }

    private List<DishDTO> getFullDishDTOs() {
        DynamoDbTable<Dish> table = enhancedClient.table(tableName, TableSchema.fromBean(Dish.class));
        List<DishDTO> dishes = new ArrayList<>();

        table.scan().items().forEach(dish -> {
            if (!dish.isAvailable()) {
                dish.setState("On stop");
            }

            DishDTO dto = DishDTO.builder()
                    .id(dish.getId())
                    .name(dish.getName())
                    .imageUrl(dish.getImage())
                    .price(dish.getPrice())
                    .weight(dish.getWeight())
                    .state(dish.getState())
                    .description(dish.getDescription())
                    .calories(dish.getCalories())
                    .carbs(dish.getCarbs())
                    .fats(dish.getFats())
                    .proteins(dish.getProteins())
                    .vitamins(dish.getVitamins())
                    .dishType(dish.getDishType())
                    .isAvailable(dish.isAvailable())
                    .isPopular(dish.isPopular())
                    .popularityScore(dish.getPopularityScore())
                    .build();

            dishes.add(dto);
        });

        return dishes;
    }

    public List<DishSmallDTO> getAllDishes() {
        return getFullDishDTOs().stream()
                .map(dto -> DishSmallDTO.builder()
                        .id(dto.getId())
                        .name(dto.getName())
                        .imageUrl(dto.getImageUrl())
                        .price(dto.getPrice())
                        .weight(dto.getWeight())
                        .state(dto.getState())
                        .build())
                .collect(Collectors.toList());
    }

    public DishDTO getDishesById(String dishId) {
        DynamoDbTable<Dish> table = enhancedClient.table(tableName, TableSchema.fromBean(Dish.class));
        List<Dish> dishes = new ArrayList<>();
        table.scan().items().forEach(dishes::add);

        for (Dish dish : dishes) {
            if (dish.getId().equalsIgnoreCase(dishId)) {
                if (!dish.isAvailable()) {
                    dish.setState("On stop");
                }
                return DishDTO.builder()
                        .id(dish.getId())
                        .name(dish.getName())
                        .price(dish.getPrice())
                        .weight(dish.getWeight())
                        .imageUrl(dish.getImage())
                        .description(dish.getDescription())
                        .calories(dish.getCalories())
                        .carbs(dish.getCarbs())
                        .dishType(dish.getDishType())
                        .fats(dish.getFats())
                        .proteins(dish.getProteins())
                        .state(dish.getState())
                        .vitamins(dish.getVitamins())
                        .build();
            }
        }
        throw new DishNotFoundException("Dish with id " + dishId + " not found");
    }

    public List<Dish> getDishCategorizedByType(String dishTypeFilter) {
        DynamoDbTable<Dish> table = enhancedClient.table(tableName, TableSchema.fromBean(Dish.class));
        List<Dish> dishes = new ArrayList<>();
        table.scan().items().forEach(dishes::add);

        for (Dish dish : dishes) {
            if (!dish.isAvailable()) {
                dish.setState("On stop");
            }
        }

        // If a dish type filter is provided, only return that category
        if (dishTypeFilter != null && !dishTypeFilter.isEmpty()) {
            List<Dish> filteredDishes = dishes.stream()
                    .filter(dish -> dish.getDishType().equalsIgnoreCase(dishTypeFilter))
                    .collect(Collectors.toList());

            if (!filteredDishes.isEmpty()) {
                return filteredDishes;
            }
        }
        return dishes;
    }


    public List<DishSmallDTO> sortDishes(String sort) {
        List<DishDTO> allDishes = getFullDishDTOs();

        if (sort == null || sort.isBlank()) {
            return convertToSmallDTO(allDishes);
        }

        String[] sortParts = sort.toLowerCase().split(",");
        if (sortParts.length != 2) {
            return convertToSmallDTO(allDishes);
        }

        String sortBy = sortParts[0];
        String sortOrder = sortParts[1];

        Comparator<DishDTO> comparator = null;

        switch (sortBy) {
            case "price":
                comparator = Comparator.comparingDouble(d -> Double.parseDouble(d.getPrice()));
                break;
            case "popularity":
                comparator = Comparator
                        .comparing(DishDTO::getIsPopular).reversed()
                        .thenComparing(Comparator.comparingInt((DishDTO d) -> Integer.parseInt(d.getPopularityScore())).reversed());
                break;

        }


        if (comparator != null && "desc".equals(sortOrder) && !"popularity".equals(sortBy)) {
            comparator = comparator.reversed();
        }
        if (comparator != null) {
            allDishes.sort(comparator);
        }

        return convertToSmallDTO(allDishes);

    }

    private List<DishSmallDTO> convertToSmallDTO(List<DishDTO> fullDishes) {
        return fullDishes.stream()
                .map(dto -> DishSmallDTO.builder()
                        .id(dto.getId())
                        .name(dto.getName())
                        .imageUrl(dto.getImageUrl())
                        .price(dto.getPrice())
                        .weight(dto.getWeight())
                        .state(dto.getState())
                        .dishType(dto.getDishType())
                        .isPopular(dto.getIsPopular())
                        .popularityScore(dto.getPopularityScore())
                        .build())
                .collect(Collectors.toList());
    }


}
