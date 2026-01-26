package com.shophub.repository;

import com.shophub.model.RefreshToken;
import com.shophub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    void deleteByToken(String token);
    void deleteByUser(User user);
}
