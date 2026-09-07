package com.net2rent.net2rent_backend.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.net2rent.net2rent_backend.model.enums.Permission;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final GuestTokenService guestTokenService;

    public JwtAuthenticationFilter(JwtService jwtService, GuestTokenService guestTokenService) {
        this.jwtService = jwtService;
        this.guestTokenService = guestTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 1. Intentar como token de STAFF
            try {
                Claims claims = jwtService.parseClaims(token);
                Long userId = ((Number) claims.get("user_id")).longValue();
                Long accountId = ((Number) claims.get("account_id")).longValue();
                String email = claims.getSubject();
                String roleName = claims.get("role", String.class);

                UserRole role = UserRole.valueOf(roleName);
                AuthUser authUser = new AuthUser(userId, accountId, email, roleName);

                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
                for (Permission permission : RolePermissions.forRole(role)) {
                    authorities.add(new SimpleGrantedAuthority(permission.name()));
                }

                var authentication = new UsernamePasswordAuthenticationToken(authUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;

            } catch (Exception e) {
                // Token de staff no válido, intentar como HUÉSPED
            }

            // 2. Intentar como token de HUÉSPED
            try {
                Claims guestClaims = guestTokenService.parseClaims(token);
                Long lodgingId = ((Number) guestClaims.get("lodging_id")).longValue();
                String lodgingRef = guestClaims.getSubject();

                GuestAuthentication guestAuth = new GuestAuthentication(lodgingId, lodgingRef);
                SecurityContextHolder.getContext().setAuthentication(guestAuth);

            } catch (Exception ex) {
                logger.warn("Token JWT rechazado: " + ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}