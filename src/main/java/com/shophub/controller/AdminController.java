package com.shophub.controller;

import com.shophub.service.AdminAuditService;
import com.shophub.service.AdminActionTokenService;
import com.shophub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAuditService adminAuditService;
    private final AdminActionTokenService adminActionTokenService;
    private final UserService userService;

    @GetMapping("/test")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> test(HttpServletRequest request) {

        adminAuditService.log(
                request.getUserPrincipal().getName(),
                "TEST_ENDPOINT",
                "/api/admin/test",
                request.getRemoteAddr()
        );

        return ResponseEntity.ok("ADMIN ACCESS OK");
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Long> confirmAction(
            @RequestParam String action,
            Principal principal,
            HttpServletRequest request
    ) {
        var token = adminActionTokenService.create(principal.getName(), action);

        adminAuditService.log(
                principal.getName(),
                "CONFIRM_ACTION",
                "/api/admin/confirm?action=" + action,
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(token.getId());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestParam Long tokenId,
            Principal principal,
            HttpServletRequest request
    ) {
        adminActionTokenService.verify(tokenId, principal.getName());
        userService.deleteUser(id);

        adminAuditService.log(
                principal.getName(),
                "DELETE_USER",
                "/api/admin/users/" + id,
                request.getRemoteAddr()
        );

        return ResponseEntity.ok().build();
    }
}
