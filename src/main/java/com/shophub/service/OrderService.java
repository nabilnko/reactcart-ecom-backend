package com.shophub.service;

import com.shophub.dto.OrderRequest;
import com.shophub.dto.GuestCheckoutRequest;
import com.shophub.exception.BadRequestException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Order;
import com.shophub.model.OrderItem;
import com.shophub.model.Product;
import com.shophub.model.User;
import com.shophub.repository.OrderRepository;
import com.shophub.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Order createOrder(OrderRequest request, User user) {
        Order order = new Order();

        // Set user info
        if (user != null) {
            order.setUser(user);
            order.setGuestOrder(false);
            order.setUserName(user.getName());
            order.setEmail(user.getEmail());
        } else {
            // Guest checkout
            order.setGuestOrder(true);
            order.setUserName(request.getFirstName() + " " + request.getLastName());
            order.setEmail(request.getEmail());
        }

        // Set order details
        order.setFirstName(request.getFirstName());
        order.setLastName(request.getLastName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setDistrict(request.getDistrict());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setDeliveryMethod(request.getDeliveryMethod());
        order.setDeliveryCharge(request.getDeliveryCharge());
        order.setComment(request.getComment());
        order.setStatus("pending");

        // Calculate total and create order items
        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        for (var itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            // ✅ CHECK STOCK AVAILABILITY BEFORE CREATING ORDER
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStock() +
                        ", Requested: " + itemRequest.getQuantity());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());

            // ✅ Store product details for order history (if those fields exist in OrderItem)
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getImage());
            orderItem.setProductDescription(product.getDescription());

            orderItems.add(orderItem);
            total += product.getPrice() * itemRequest.getQuantity();

            // ✅ RESERVE STOCK IMMEDIATELY WHEN ORDER IS PLACED
            product.setStock(product.getStock() - itemRequest.getQuantity());
            product.setInStock(product.getStock() > 0);
            productRepository.save(product);
        }

        order.setItems(orderItems);
        order.setTotal(total + (request.getDeliveryCharge() != null ? request.getDeliveryCharge() : 0));

        Order savedOrder = orderRepository.save(order);

        try {
            Order dbOrder = orderRepository.findById(savedOrder.getId()).orElse(savedOrder);
            if (dbOrder.getItems() != null) {
                dbOrder.getItems().size();
            }

            String customerName = savedOrder.getUserName();
            if (customerName == null || customerName.isBlank()) {
                String firstName = savedOrder.getFirstName() == null ? "" : savedOrder.getFirstName().trim();
                String lastName = savedOrder.getLastName() == null ? "" : savedOrder.getLastName().trim();
                customerName = (firstName + " " + lastName).trim();
            }

            emailService.sendOrderInvoiceEmail(dbOrder, customerName);
            emailService.sendAdminNewOrderNotification(dbOrder, customerName);
        } catch (Exception e) {
            log.error("Order email failed but order created", e);
        }

        return savedOrder;
    }

    @Transactional
    public Order createGuestOrder(GuestCheckoutRequest request) {
        OrderRequest orderRequest = new OrderRequest();
        String fullName = request.getFullName() == null ? "" : request.getFullName().trim();
        String[] nameParts = fullName.split("\\s+", 2);
        orderRequest.setFirstName(nameParts.length > 0 ? nameParts[0] : fullName);
        orderRequest.setLastName(nameParts.length > 1 ? nameParts[1] : "");

        orderRequest.setEmail(request.getEmail());
        orderRequest.setAddress(request.getShippingAddress());
        orderRequest.setItems(request.getItems());

        return createOrder(orderRequest, null);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @Transactional
    public Order updateOrderStatus(Long id, String status) {
        Order order = getOrderById(id);
        String oldStatus = order.getStatus();

        // ✅ RESTORE STOCK IF ORDER IS CANCELLED
        // Only restore stock if transitioning TO cancelled status
        if ("cancelled".equalsIgnoreCase(status) && !"cancelled".equalsIgnoreCase(oldStatus)) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();

                // Restore the quantity back to stock
                product.setStock(product.getStock() + item.getQuantity());
                product.setInStock(product.getStock() > 0);
                productRepository.save(product);
            }
        }

        // ✅ NO STOCK CHANGE for other status updates (processing, shipped, delivered)
        // Stock was already reserved when order was created

        order.setStatus(status);
        return orderRepository.save(order);
    }
}
