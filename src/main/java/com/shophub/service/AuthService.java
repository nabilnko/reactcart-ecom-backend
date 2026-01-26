package com.shophub.service;

import com.shophub.dto.AuthResponse;
import com.shophub.dto.LoginRequest;
import com.shophub.dto.RefreshTokenRequest;
import com.shophub.exception.UnauthorizedException;
import com.shophub.model.RefreshToken;
import com.shophub.model.Role;
import com.shophub.model.User;
import com.shophub.repository.UserRepository;
import com.shophub.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    // ===================== LOGIN =====================
        @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid credentials")
                );

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {

            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 5) {
                user.setAccountLockedUntil(
                        LocalDateTime.now().plusMinutes(15)
                );
            }

            userRepository.saveAndFlush(user); // 🔥 force DB write
            throw ex;
        }

        // ✅ SUCCESS → RESET COUNTERS
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);

                if (user.getRole() == Role.ROLE_ADMIN) {
                        user.setActiveAdminSession(UUID.randomUUID().toString());
                }

        userRepository.saveAndFlush(user);

        return tokenProvider.generateTokens(user);
    }

    // ===================== REFRESH TOKEN =====================
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        if (request == null || request.getRefreshToken() == null
                || request.getRefreshToken().isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }

        // 1️⃣ Validate refresh token
        RefreshToken storedToken =
                refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        User user = storedToken.getUser();

        // 2️⃣ Revoke old refresh token
        refreshTokenService.revokeRefreshToken(storedToken);

        // 3️⃣ Generate new tokens
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        // 4️⃣ Save new refresh token
        refreshTokenService.createRefreshToken(
                user,
                newRefreshToken,
                tokenProvider.getRefreshTokenValidity()
        );

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

        @Transactional
        public void logout(String refreshToken) {

                if (refreshToken == null || refreshToken.isBlank()) {
                        return; // idempotent logout
                }

                refreshTokenService.revokeRefreshToken(refreshToken);
        }
}
