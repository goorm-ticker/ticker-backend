package com.goorm.ticker.restaurant.repository;

import com.goorm.ticker.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    @Query("SELECT r.restaurantId FROM Restaurant r WHERE r.restaurantId IN :ids")
    List<Long> findExistingIds(@Param("ids") List<Long> ids);
}
