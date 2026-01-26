package com.shophub.service;

import com.shophub.exception.UnauthorizedException;
import com.shophub.model.AdminActionToken;
import com.shophub.repository.AdminActionTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminActionTokenService {

    private final AdminActionTokenRepository repository;

    public AdminActionToken create(String adminEmail, String action) {

        AdminActionToken token = new AdminActionToken();
        token.setAdminEmail(adminEmail);
        token.setAction(action);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setUsed(false);

        return repository.save(token);
    }

    public AdminActionToken verify(Long tokenId, String adminEmail) {

        AdminActionToken token = repository.findByIdAndUsedFalse(tokenId)
                .orElseThrow(() -> new UnauthorizedException("Invalid admin action token"));

        if (token.getAdminEmail() == null || !token.getAdminEmail().equals(adminEmail)) {
            throw new UnauthorizedException("Token owner mismatch");
        }

        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Admin action token expired");
        }

        token.setUsed(true);
        return repository.save(token);
    }
}
