package com.net2rent.net2rent_backend.security;

import java.util.Date;
import javax.crypto.SecretKey;

import com.net2rent.net2rent_backend.config.JwtProperties;
import com.net2rent.net2rent_backend.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

    public String generateToken(AppUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + props.expirationMs());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("user_id", user.getId())
                .claim("account_id", user.getAccount().getId())
                .claim("role", user.getRole().name())
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
