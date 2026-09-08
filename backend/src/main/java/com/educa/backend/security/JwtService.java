package com.educa.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.educa.backend.config.EducaProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Émission et vérification des JWT d'accès (HMAC-SHA256).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlSeconds;

    public JwtService(EducaProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = properties.jwt().accessTtlSeconds();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public String generateAccessToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * @return les claims si le token est valide, {@code null} sinon.
     */
    public Claims parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
