package com.shophub.service;

import com.shophub.dto.LoginRequest;
import com.shophub.dto.RegisterRequest;
import com.shophub.dto.AuthResponse;
import com.shophub.exception.BadRequestException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.exception.UnauthorizedException;
import com.shophub.model.Role;
import com.shophub.model.User;
import com.shophub.repository.UserRepository;
import com.shophub.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_CUSTOMER);
        user.setCreatedAt(LocalDateTime.now());

        user = userRepository.save(user);

        String accessToken =
            jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());

        String refreshToken =
            jwtTokenProvider.generateRefreshToken(user.getEmail());

        refreshTokenService.createRefreshToken(
            user,
            refreshToken,
            jwtTokenProvider.getRefreshTokenValidity()
        );

        return new AuthResponse(
            accessToken,
            refreshToken,
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole().name()
        );
    }
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }


    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken =
            jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());

        String refreshToken =
                jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Persist refresh token (single-token policy)
        refreshTokenService.createRefreshToken(
            user,
            refreshToken,
            jwtTokenProvider.getRefreshTokenValidity()
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getName(),
                user.getEmail(),
            user.getRole().name()
        );
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }


}
