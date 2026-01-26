package com.shophub.service;

import com.shophub.model.AdminAuditLog;
import com.shophub.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository repository;

    public void log(
            String adminEmail,
            String action,
            String resource,
            String ipAddress
    ) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminEmail(adminEmail);
        log.setAction(action);
        log.setResource(resource);
        log.setTimestamp(LocalDateTime.now());
        log.setIpAddress(ipAddress);

        repository.save(log);
    }
}
