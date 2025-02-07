package com.goorm.ticker.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goorm.ticker.restaurant.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}
