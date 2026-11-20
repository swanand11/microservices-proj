package com.portfolens.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET must be configured with a Base64 key of at least 32 bytes");
        try { this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); }
        catch (RuntimeException ex) { throw new IllegalStateException("JWT_SECRET must be a valid Base64 key of at least 32 bytes", ex); }
        this.expirationMs = expirationMs;
        if (expirationMs <= 0) throw new IllegalStateException("JWT_EXPIRATION_MS must be positive");
    }
    public String generate(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId.toString()).claim("email", email).issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expirationMs))).signWith(key).compact();
    }
    public UUID extractUserId(String token) { return UUID.fromString(parse(token).getSubject()); }
    public boolean isValid(String token) { try { parse(token); return true; } catch (RuntimeException ex) { return false; } }
    public long getExpirationSeconds() { return expirationMs / 1000; }
    private Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
