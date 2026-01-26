package com.shophub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserProfile {
    private Long id;
    private String name;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
