package com.hmall.user.infrastructure;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token 生成服务（基础设施层）。
 */
@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenService(
            @Value("${jwt.secret:hmall-default-secret-key-at-least-256-bits-for-hs256}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String createToken(Long userId, String username) {
        return Jwts.builder()
            .subject(username)
            .claim("userId", userId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey)
            .compact();
    }
}
