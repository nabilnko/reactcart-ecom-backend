package com.shophub.repository;

import com.shophub.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // ✅ Find all reviews for a product (ordered by newest first)
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // ✅ Find all reviews for a product (unordered)
    List<Review> findByProductId(Long productId);

    // ✅ Find all reviews by a user (ordered by newest first)
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ✅ Check if a user already reviewed a product
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    // ✅ Count reviews for a product
    long countByProductId(Long productId);
}
