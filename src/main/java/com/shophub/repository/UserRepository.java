package com.shophub.repository;

import com.shophub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // ❌ REMOVE THIS - User doesn't have username field
    // Optional<User> findByUsername(String username);

    Boolean existsByEmail(String email);

    // Count only customers (exclude admins)
    Long countByRole(String role);

    // NEW: Find all users by role
    List<User> findByRole(String role);
}
