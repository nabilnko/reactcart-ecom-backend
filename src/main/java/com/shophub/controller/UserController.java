package com.shophub.controller;

import com.shophub.model.User;
import com.shophub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getUserCount() {
        // Count only users with CUSTOMER role (exclude admins)
        long count = userRepository.countByRole("CUSTOMER");
        return ResponseEntity.ok(count);
    }

    // NEW: Get list of all customers
    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getCustomers() {
        List<User> customers = userRepository.findByRole("CUSTOMER");
        return ResponseEntity.ok(customers);
    }

    // ✅ FIXED: Update user profile (WITHOUT phone)
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates, Authentication authentication) {
        try {
            String email = authentication.getName();

            Optional<User> userOptional = userRepository.findByEmail(email);
            if (!userOptional.isPresent()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            User user = userOptional.get();

            // Update basic info
            if (updates.containsKey("name")) {
                user.setName(updates.get("name"));
            }
            if (updates.containsKey("email")) {
                // Check if email is already taken by another user
                Optional<User> existingUserOptional = userRepository.findByEmail(updates.get("email"));
                if (existingUserOptional.isPresent() && !existingUserOptional.get().getId().equals(user.getId())) {
                    return ResponseEntity.badRequest().body("Email already in use");
                }
                user.setEmail(updates.get("email"));
            }

            // ❌ REMOVED PHONE LOGIC

            // Update password if provided
            if (updates.containsKey("currentPassword") && updates.containsKey("newPassword")) {
                if (!passwordEncoder.matches(updates.get("currentPassword"), user.getPassword())) {
                    return ResponseEntity.badRequest().body("Current password is incorrect");
                }
                user.setPassword(passwordEncoder.encode(updates.get("newPassword")));
            }

            User updatedUser = userRepository.save(user);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error updating profile: " + e.getMessage());
        }
    }
}
