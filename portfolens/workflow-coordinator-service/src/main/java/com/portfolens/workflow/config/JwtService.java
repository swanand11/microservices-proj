package com.portfolens.workflow.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    private final SecretKey key;
    public JwtService(@Value("${app.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET must be configured");
        try { key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); }
        catch (RuntimeException ex) { throw new IllegalStateException("JWT_SECRET must be a valid Base64 key", ex); }
    }
    public UUID userId(String token) { return UUID.fromString(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject()); }
}
