package com.net2rent.net2rent_backend.security;

import java.util.Date;
import javax.crypto.SecretKey;


import org.springframework.stereotype.Service;

import com.net2rent.net2rent_backend.config.GuestTokenProperties;
import com.net2rent.net2rent_backend.model.Lodging;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class GuestTokenService {

    private final GuestTokenProperties props;

    public GuestTokenService(GuestTokenProperties props) {
        this.props = props;
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

     public String generateToken(Lodging lodging) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + props.expirationMs());

        return Jwts.builder()
                .subject(lodging.getRef())
                .claim("lodging_id", lodging.getId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
