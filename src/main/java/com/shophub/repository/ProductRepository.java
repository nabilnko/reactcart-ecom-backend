package com.shophub.repository;

import com.shophub.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Existing methods - keep these
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByBadge(String badge);

    // NEW: Custom query for Category entity relationship
    @Query("SELECT p FROM Product p WHERE p.categoryEntity.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);
}
