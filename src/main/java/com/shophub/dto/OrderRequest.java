package com.shophub.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String district;
    private String paymentMethod;
    private String deliveryMethod;
    private Double deliveryCharge;
    private String comment;
    private List<OrderItemRequest> items;
}
