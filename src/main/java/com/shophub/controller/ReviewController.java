package com.shophub.controller;

import com.shophub.dto.ReviewRequest;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.exception.UnauthorizedException;
import com.shophub.model.Order;
import com.shophub.model.Product;
import com.shophub.model.Review;
import com.shophub.model.User;
import com.shophub.repository.OrderRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.ReviewRepository;
import com.shophub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    // ✅ Get all reviews for a product
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getProductReviews(@PathVariable Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return ResponseEntity.ok(reviews);
    }

    // ✅ Get product rating stats
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getProductRatingStats(@PathVariable Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

        Map<String, Object> stats = new HashMap<>();
        if (reviews.isEmpty()) {
            stats.put("averageRating", 0.0);
            stats.put("reviewCount", 0);
            stats.put("purchaseCount", 0);
            return ResponseEntity.ok(stats);
        }

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // Count unique purchases for this product (Delivered orders only)
        long purchaseCount = orderRepository.findAll().stream()
                .filter(order -> "delivered".equalsIgnoreCase(order.getStatus()))
                .flatMap(order -> order.getItems().stream())
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .count();

        stats.put("averageRating", Math.round(averageRating * 10.0) / 10.0);
        stats.put("reviewCount", reviews.size());
        stats.put("purchaseCount", purchaseCount);

        return ResponseEntity.ok(stats);
    }

    // ✅ Submit a review (with verified purchase check)
    @PostMapping("/product/{productId}")
    public ResponseEntity<?> submitReview(
            @PathVariable Long productId,
            @RequestBody ReviewRequest request,
            Authentication authentication) {

        // ✅ FIXED: Get email from authentication (not username)
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid user"));

        // Check if user purchased this product
        boolean hasPurchased = orderRepository.findAll().stream()
                .filter(order -> order.getUser() != null && order.getUser().getId().equals(user.getId()))
                .filter(order -> "delivered".equalsIgnoreCase(order.getStatus()))
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> item.getProduct() != null && item.getProduct().getId().equals(productId));

        if (!hasPurchased) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "You can only review products you've purchased"));
        }

        // Check if user already reviewed this product
        boolean alreadyReviewed = reviewRepository.existsByProductIdAndUserId(productId, user.getId());
        if (alreadyReviewed) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "You have already reviewed this product"));
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        // ✅ FIXED: Use name field instead of username
        review.setUsername(user.getName());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setVerified(true);
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);

        return ResponseEntity.ok(Map.of("message", "Review submitted successfully"));
    }

    // ✅ Get user's reviews
    @GetMapping("/my-reviews")
    public ResponseEntity<List<Review>> getMyReviews(Authentication authentication) {
        // ✅ FIXED: Get email from authentication (not username)
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid user"));

        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(reviews);
    }

    // ✅ Delete a review (user can only delete their own)
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {

        // ✅ FIXED: Get email from authentication (not username)
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid user"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "You can only delete your own reviews"));
        }

        reviewRepository.delete(review);
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }
}
