package com.shophub.controller;

import com.shophub.dto.UserProfile;
import com.shophub.exception.BadRequestException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Role;
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

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = new UserProfile(
                user.getId(),
                user.getName(),
                user.getEmail(),
            user.getRole().name(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getUserCount() {
        // Count only users with CUSTOMER role (exclude admins)
        long count = userRepository.countByRole(Role.ROLE_CUSTOMER);
        return ResponseEntity.ok(count);
    }

    // NEW: Get list of all customers
    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getCustomers() {
        List<User> customers = userRepository.findByRole(Role.ROLE_CUSTOMER);
        return ResponseEntity.ok(customers);
    }

    // ✅ FIXED: Update user profile (WITHOUT phone)
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates, Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update basic info
        if (updates.containsKey("name")) {
            user.setName(updates.get("name"));
        }
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email");
            User existingUser = userRepository.findByEmail(newEmail).orElse(null);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(newEmail);
        }

        // Update password if provided
        if (updates.containsKey("currentPassword") && updates.containsKey("newPassword")) {
            if (!passwordEncoder.matches(updates.get("currentPassword"), user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(updates.get("newPassword")));
        }

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }
}
