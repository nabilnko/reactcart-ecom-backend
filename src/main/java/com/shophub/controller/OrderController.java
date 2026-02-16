package com.shophub.controller;

import com.shophub.dto.OrderRequest;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.exception.UnauthorizedException;
import com.shophub.model.Order;
import com.shophub.model.User;
import com.shophub.service.OrderService;
import com.shophub.service.UserService;
import com.shophub.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    // Create order (Guest or Authenticated)
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request,
            Authentication authentication) {
        User user = null;
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                user = (User) principal;
            } else {
                String email = authentication.getName();
                user = userService.getUserByEmail(email);
            }
        }

        Order order = orderService.createOrder(request, user);
        return ResponseEntity.ok(order);
    }

    // ✅ FIXED: Get user's own orders WITHOUT @RequestParam String token
    @GetMapping("/my-orders")
    public ResponseEntity<?> getUserOrders(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Please log in to view orders");
        }

        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            email = ((User) principal).getEmail();
        } else {
            email = authentication.getName();
        }
        List<Order> orders = orderRepository.findByEmailOrderByCreatedAtDesc(email);
        return ResponseEntity.ok(orders);
    }

    // ✅ Get all orders (Admin only)
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(orders);
    }

    // Get order by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return ResponseEntity.ok(order);
    }

    // Update order status (Admin only)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
        return ResponseEntity.ok(order);
    }
}
