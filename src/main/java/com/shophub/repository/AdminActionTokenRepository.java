package com.shophub.repository;

import com.shophub.model.AdminActionToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminActionTokenRepository extends JpaRepository<AdminActionToken, Long> {

    Optional<AdminActionToken> findByIdAndUsedFalse(Long id);
}
