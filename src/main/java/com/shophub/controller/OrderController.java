package com.shophub.controller;

import com.shophub.dto.OrderRequest;
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
@CrossOrigin(origins = "http://localhost:3000")
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
        try {
            User user = null;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                user = userService.getUserByEmail(email);
            }

            Order order = orderService.createOrder(request, user);
            System.out.println("✅ Order created with ID: " + order.getId());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ✅ FIXED: Get user's own orders WITHOUT @RequestParam String token
    @GetMapping("/my-orders")
    public ResponseEntity<?> getUserOrders(Authentication authentication) {
        try {
            // ✅ Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("❌ Authentication is null or not authenticated");
                return ResponseEntity.status(401).body("Please log in to view orders");
            }

            String email = authentication.getName();
            System.out.println("🔍 Looking for orders with email: " + email);

            List<Order> orders = orderRepository.findByUserEmailOrderByCreatedAtDesc(email);
            System.out.println("✅ Found " + orders.size() + " orders");

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ✅ Get all orders (Admin only)
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllOrders() {
        try {
            System.out.println("📡 Admin fetching all orders...");
            List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
            System.out.println("✅ Found " + orders.size() + " total orders");
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Get order by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Update order status (Admin only)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            order.setStatus(status);
            orderRepository.save(order);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
