package com.shophub.controller;

import com.shophub.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // TEMPORARY: remove TestEmailController after setup
public class TestEmailController { // TEMPORARY: remove this class after setup

    private final EmailService emailService; // TEMPORARY: unused now; keep only if you add more test endpoints

    public TestEmailController(EmailService emailService) { // TEMPORARY: remove with the class
        this.emailService = emailService; // TEMPORARY: remove with the class
    }

    @GetMapping("/generate-admin-password") // TEMPORARY: remove endpoint after generating password hash
    public String generateAdminPassword() { // TEMPORARY: remove endpoint after generating password hash
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder() // TEMPORARY: remove endpoint after generating password hash
                .encode("Admin@123"); // TEMPORARY: remove endpoint after generating password hash
    }
}
