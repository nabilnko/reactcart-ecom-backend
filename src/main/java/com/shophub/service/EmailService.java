package com.shophub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${BREVO_API_KEY}")
    private String apiKey;

    @Value("${MAIL_FROM}")
    private String mailFrom;

    @Value("${MAIL_FROM_NAME}")
    private String mailFromName;

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of(
                    "email", mailFrom,
                    "name", mailFromName
            ));
            payload.put("to", new Object[]{
                    Map.of("email", to)
            });
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            HttpEntity<String> request = new HttpEntity<>(
                    new ObjectMapper().writeValueAsString(payload),
                    headers
            );

            ResponseEntity<String> response =
                    restTemplate.postForEntity(BREVO_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Brevo email failed: " + response.getBody());
            }

        } catch (Exception e) {
            throw new RuntimeException("Email sending failed", e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String firstName) {
        String subject = "Welcome to Kiara Lifestyle 💖";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Hi %s 👋</h2>
                <p>Welcome to <strong>Kiara Lifestyle</strong>!</p>
                <p>We’re excited to have you with us.</p>
                <p>Start exploring our latest collections now.</p>
                <br>
                <a href="https://kiaralifestyle.com"
                   style="background-color:#000;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px;">
                   Shop Now
                </a>
                <br><br>
                <p>With love 💕<br>Kiara Lifestyle Team</p>
            </div>
            """.formatted(firstName);

        sendEmail(toEmail, subject, htmlContent);
    }
}
