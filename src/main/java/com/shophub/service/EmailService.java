package com.shophub.service;

import com.shophub.model.Order;
import com.shophub.model.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
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

    public void sendOrderConfirmationEmail(Order order, String customerName) {
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }
        if (order.getEmail() == null || order.getEmail().isBlank()) {
            throw new IllegalArgumentException("order email must not be blank");
        }
        if (order.getId() == null) {
            throw new IllegalArgumentException("order id must not be null");
        }

        String safeCustomerName = (customerName == null || customerName.isBlank()) ? "there" : customerName;
        String subject = "Your Order #" + order.getId() + " is Confirmed 🎉";

        List<OrderItem> items = order.getItems();
        double itemsSubtotal = 0.0;

        StringBuilder rows = new StringBuilder();
        if (items != null) {
            for (OrderItem item : items) {
                if (item == null) continue;
                String name = item.getProductName();
                if ((name == null || name.isBlank()) && item.getProduct() != null) {
                    name = item.getProduct().getName();
                }
                String safeName = esc(name == null ? "Item" : name);

                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                double unitPrice = item.getPrice() == null ? 0.0 : item.getPrice();
                double lineTotal = unitPrice * qty;
                itemsSubtotal += lineTotal;

                rows.append("<tr>")
                        .append("<td style=\"padding:8px;border:1px solid #ddd;\">" + safeName + "</td>")
                        .append("<td style=\"padding:8px;border:1px solid #ddd;text-align:center;\">" + qty + "</td>")
                        .append("<td style=\"padding:8px;border:1px solid #ddd;text-align:right;\">৳ " + money(unitPrice) + "</td>")
                        .append("<td style=\"padding:8px;border:1px solid #ddd;text-align:right;\">৳ " + money(lineTotal) + "</td>")
                        .append("</tr>");
            }
        }

        double deliveryCharge = order.getDeliveryCharge() == null ? 0.0 : order.getDeliveryCharge();
        double grandTotal = order.getTotal() == null ? 0.0 : order.getTotal();

        String safeDeliveryMethod = esc(blankAsDash(order.getDeliveryMethod()));
        String safePaymentMethod = esc(blankAsDash(order.getPaymentMethod()));
        String safePhone = esc(blankAsDash(order.getPhone()));
        String safeAddress = esc(blankAsDash(order.getAddress()));
        String safeDistrict = esc(blankAsDash(order.getDistrict()));

        String estimatedDelivery = esc(estimateDelivery(order.getDeliveryMethod()));

        String htmlContent = """
            <div style=\"font-family: Arial, sans-serif;\">
                <h2>Hi %s 👋</h2>
                <p>Thank you for shopping with <strong>Kiara Lifestyle</strong>.</p>

                <h3>Order Summary</h3>
                <p><strong>Order ID:</strong> #%d</p>
                <p><strong>Status:</strong> %s</p>
                <p><strong>Payment Method:</strong> %s</p>
                <p><strong>Delivery Method:</strong> %s</p>
                <p><strong>Estimated Delivery:</strong> %s</p>

                <h3>Shipping Details</h3>
                <p><strong>Phone:</strong> %s</p>
                <p><strong>Address:</strong> %s</p>
                <p><strong>District:</strong> %s</p>

                <h3>Items</h3>
                <table style=\"border-collapse:collapse;width:100%;\">
                    <thead>
                        <tr>
                            <th style=\"padding:8px;border:1px solid #ddd;text-align:left;\">Product</th>
                            <th style=\"padding:8px;border:1px solid #ddd;text-align:center;\">Qty</th>
                            <th style=\"padding:8px;border:1px solid #ddd;text-align:right;\">Unit</th>
                            <th style=\"padding:8px;border:1px solid #ddd;text-align:right;\">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>

                <h3>Total</h3>
                <p><strong>Items Subtotal:</strong> ৳ %s</p>
                <p><strong>Delivery Charge:</strong> ৳ %s</p>
                <p><strong>Grand Total:</strong> ৳ %s</p>

                <p>Your order is now being processed.</p>
                <p>We’ll notify you once it ships 🚚</p>

                <hr>
                <p style=\"font-size:12px;color:gray;\">
                    Kiara Lifestyle<br>
                    support@kiaralifestyle.com
                </p>
            </div>
            """.formatted(
                esc(safeCustomerName),
                order.getId(),
                esc(blankAsDash(order.getStatus())),
                safePaymentMethod,
                safeDeliveryMethod,
                estimatedDelivery,
                safePhone,
                safeAddress,
                safeDistrict,
                rows.toString(),
                money(itemsSubtotal),
                money(deliveryCharge),
                money(grandTotal)
        );

        sendEmail(order.getEmail(), subject, htmlContent);
    }

    private static String money(double amount) {
        return String.format("%.2f", amount);
    }

    private static String blankAsDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private static String estimateDelivery(String deliveryMethod) {
        if (deliveryMethod == null) return "-";
        String v = deliveryMethod.trim().toLowerCase();
        if (v.isBlank()) return "-";
        if (v.contains("express")) return "1-2 days (based on delivery method)";
        if (v.contains("standard") || v.contains("regular")) return "3-5 days (based on delivery method)";
        return "Based on delivery method";
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public void sendOrderConfirmationEmail(
            String to,
            String customerName,
            Long orderId,
            Double totalAmount
    ) {

        String subject = "Your Order #" + orderId + " is Confirmed 🎉";

        String safeCustomerName = (customerName == null || customerName.isBlank()) ? "there" : customerName;
        double safeTotalAmount = totalAmount == null ? 0.0 : totalAmount;

        String htmlContent = """
            <div style=\"font-family: Arial, sans-serif;\">
                <h2>Hi %s 👋</h2>
                <p>Thank you for shopping with <strong>Kiara Lifestyle</strong>.</p>

                <h3>Order Details:</h3>
                <p><strong>Order ID:</strong> #%d</p>
                <p><strong>Total Amount:</strong> ৳ %.2f</p>

                <p>Your order is now being processed.</p>

                <br>
                <p>The delivery wil be done within 3-5 days 🚚</p>

                <hr>
                <p style=\"font-size:12px;color:gray;\">
                    Kiara Lifestyle<br>
                    support@kiaralifestyle.com
                </p>
            </div>
            """.formatted(safeCustomerName, orderId, safeTotalAmount);

        sendEmail(to, subject, htmlContent);
    }
}
