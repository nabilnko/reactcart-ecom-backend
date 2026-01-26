package com.shophub.security;

import com.shophub.exception.UnauthorizedException;
import com.shophub.model.User;
import com.shophub.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);
                String sessionId = jwtTokenProvider.getSessionFromToken(jwt);

                if (email == null || role == null) {
                    throw new IllegalArgumentException("Missing required JWT claims");
                }

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);

                if (authority.equals(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new UnauthorizedException("Admin not found"));

                    if (sessionId == null || sessionId.isBlank() ||
                            !Objects.equals(user.getActiveAdminSession(), sessionId)) {
                        throw new UnauthorizedException("Admin session invalidated");
                    }
                }

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(authority)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String message = (e.getMessage() == null || e.getMessage().isBlank())
                    ? "Invalid or expired token"
                    : e.getMessage().replace("\"", "\\\"");
            response.getWriter().write("{\"error\": \"" + message + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
