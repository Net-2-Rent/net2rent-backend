package com.net2rent.net2rent_backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class GuestTokenAuthenticationFilter extends OncePerRequestFilter {
    private final GuestTokenService guestTokenService;

    public GuestTokenAuthenticationFilter(GuestTokenService guestTokenService) {
        this.guestTokenService = guestTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = guestTokenService.parseClaims(token);
                Long lodgingId = ((Number) claims.get("lodging_id")).longValue();

                GuestPrincipal guestPrincipal = new GuestPrincipal(lodgingId);

                var authentication = new UsernamePasswordAuthenticationToken(guestPrincipal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                logger.debug("Token de huésped no válido para esta petición: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);

    }
}
