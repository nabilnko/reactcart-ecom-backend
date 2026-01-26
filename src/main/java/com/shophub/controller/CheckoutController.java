package com.shophub.controller;

import com.shophub.dto.GuestCheckoutRequest;
import com.shophub.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout/guest")
    public ResponseEntity<?> guestCheckout(
            @Valid @RequestBody GuestCheckoutRequest request
    ) {
        return ResponseEntity.ok(
                orderService.createGuestOrder(request)
        );
    }
}
