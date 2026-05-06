package com.api.distr.docs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT service dedicated to user authentication.
 * Uses the same onlyoffice.jwt.secret already configured in application-uat.properties.
 */
@Service
public class UserJwtService {

    @Value("${onlyoffice.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs; // default 24 hours

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)),
                          SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return validateAndExtract(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) validateAndExtract(token).get("role");
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = validateAndExtract(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
