package com.net2rent.net2rent_backend.security;

import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class GuestAuthentication implements Authentication {

    private final Long lodgingId;
    private final String lodgingRef;

    public GuestAuthentication(Long lodgingId, String lodgingRef) {
        this.lodgingId = lodgingId;
        this.lodgingRef = lodgingRef;
    }

    public Long getLodgingId() { return lodgingId; }
    public String getLodgingRef() { return lodgingRef; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    @Override
    public Object getCredentials() { return null; }

    @Override
    public Object getDetails() { return null; }

    @Override
    public Object getPrincipal() { return this; }

    @Override
    public boolean isAuthenticated() { return true; }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {}

    @Override
    public String getName() { return lodgingRef; }
}