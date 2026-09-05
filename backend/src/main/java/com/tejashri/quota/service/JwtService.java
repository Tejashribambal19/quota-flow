package com.tejashri.quota.service;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tejashri.quota.domain.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMilliseconds;

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiration =
                now.plusMillis(expirationMilliseconds);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .claim(
                        "tenantId",
                        user.getTenant() == null
                                ? null
                                : user.getTenant().getId().toString()
                )
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(
            String token,
            AppUser user
    ) {
        Claims claims = extractClaims(token);

        return claims.getSubject().equalsIgnoreCase(user.getEmail())
                && claims.getExpiration().after(new Date())
                && user.isActive();
    }

    public long getExpirationSeconds() {
        return expirationMilliseconds / 1000;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}