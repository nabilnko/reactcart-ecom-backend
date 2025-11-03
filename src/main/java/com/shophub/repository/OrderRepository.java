package com.shophub.repository;

import com.shophub.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ✅ FIXED: Use o.user.id instead of o.userId
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
            "FROM Order o JOIN o.items oi " +
            "WHERE o.user.id = :userId AND oi.product.id = :productId AND o.status = 'delivered'")
    Boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    // Find orders by user ID
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find orders by status
    List<Order> findByStatusOrderByCreatedAtDesc(String status);

    // Find all orders
    List<Order> findAllByOrderByCreatedAtDesc();

    // Find orders by user email
    List<Order> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
