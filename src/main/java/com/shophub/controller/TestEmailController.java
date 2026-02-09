package com.shophub.controller;

import com.shophub.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestEmailController {

    private final EmailService emailService;

    public TestEmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/api/test/email")
    public String testEmail() {
        emailService.sendEmail(
                "nabilnko11@gmail.com",
                "Kiara Lifestyle – Email Test ✅",
                "<h2>Email system is LIVE 🎉</h2><p>This email was sent from production.</p>"
        );
        return "Email sent successfully";
    }
}
