package com.shophub.controller;

import com.shophub.model.PasswordResetToken;
import com.shophub.model.User;
import com.shophub.repository.PasswordResetTokenRepository;
import com.shophub.repository.UserRepository;
import com.shophub.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetController(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepo,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {

        User user = userRepository.findByEmail(email)
                .orElse(null);

        // Do NOT reveal if user exists
        if (user == null) {
            return ResponseEntity.ok("If email exists, reset link sent");
        }

        tokenRepo.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));

        tokenRepo.save(resetToken);

        String resetLink =
                "https://kiaralifestyle.com/reset-password?token=" + token;

        emailService.sendEmail(
                user.getEmail(),
                "Reset your Kiara Lifestyle password",
                """
                <h2>Password Reset</h2>
                <p>Click the link below to reset your password:</p>
                <a href=\"%s\">Reset Password</a>
                <p>This link expires in 30 minutes.</p>
                """.formatted(resetLink)
        );

        return ResponseEntity.ok("If email exists, reset link sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepo.delete(resetToken);

        return ResponseEntity.ok("Password reset successful");
    }
}
