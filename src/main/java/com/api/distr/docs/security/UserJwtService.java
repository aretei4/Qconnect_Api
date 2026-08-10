package com.api.distr.docs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * JWT service for user authentication — signing algorithm is CONFIGURABLE:
 *
 *   app.jwt.algorithm=HS256   (default) symmetric, uses onlyoffice.jwt.secret
 *   app.jwt.algorithm=RS256   asymmetric, uses an RSA private/public key pair
 *
 * RS256 key locations (PEM format, `classpath:` prefix or a plain file path):
 *   app.jwt.private-key=file:keys/jwt-private.pem     # PKCS#8  — needed to SIGN tokens
 *   app.jwt.public-key=classpath:keys/jwt-public.pem  # X.509   — needed to VERIFY tokens
 *
 * A verification-only node (e.g. a read replica) may configure just the public key.
 *
 * Generate a key pair:
 *   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
 *   openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem
 */
@Service
public class UserJwtService {

    private static final Logger log = LoggerFactory.getLogger(UserJwtService.class);

    @Value("${app.jwt.algorithm:HS256}")
    private String algorithm;

    @Value("${onlyoffice.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.private-key:}")
    private String privateKeyLocation;

    @Value("${app.jwt.public-key:}")
    private String publicKeyLocation;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs; // default 24 hours

    // Lazily-loaded RSA keys (only used when algorithm=RS256)
    private volatile PrivateKey privateKey;
    private volatile PublicKey  publicKey;

    private boolean isRsa() {
        return "RS256".equalsIgnoreCase(algorithm);
    }

    // ── Key loading ───────────────────────────────────────────────────────────

    /**
     * Reads a PEM key and returns its Base64-decoded body. Accepts:
     *   classpath:keys/jwt-private.pem   → jar/classpath only
     *   file:/opt/app/keys/…             → filesystem only
     *   keys/jwt-private.pem (plain)     → filesystem first, then classpath fallback
     * The plain-path fallback lets the same config work whether the key sits next to
     * the jar on disk OR is bundled inside the jar (survives CWD differences on Unix).
     */
    private byte[] readPem(String location) throws Exception {
        String pem = null;

        if (location.startsWith("classpath:")) {
            pem = readClasspath(location.substring("classpath:".length()));
        } else if (location.startsWith("file:")) {
            pem = Files.readString(Path.of(location.substring("file:".length())), StandardCharsets.UTF_8);
        } else {
            // plain path: try filesystem, then fall back to the classpath (bundled jar)
            Path p = Path.of(location);
            if (Files.exists(p)) {
                pem = Files.readString(p, StandardCharsets.UTF_8);
            } else {
                pem = readClasspath(location);
                if (pem == null)
                    throw new java.io.FileNotFoundException(
                        "JWT key not found on filesystem (" + p.toAbsolutePath() +
                        ") or classpath (" + location + ")");
            }
        }

        String body = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private String readClasspath(String path) throws java.io.IOException {
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) return null;
        try (var in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            synchronized (this) {
                if (privateKey == null) {
                    if (privateKeyLocation == null || privateKeyLocation.isBlank())
                        throw new IllegalStateException(
                            "app.jwt.algorithm=RS256 but app.jwt.private-key is not configured — cannot sign tokens");
                    try {
                        privateKey = KeyFactory.getInstance("RSA")
                                .generatePrivate(new PKCS8EncodedKeySpec(readPem(privateKeyLocation)));
                        log.info("JWT: loaded RSA private key from {}", privateKeyLocation);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                            "Failed to load JWT private key from " + privateKeyLocation +
                            " (expecting PKCS#8 PEM): " + e.getMessage(), e);
                    }
                }
            }
        }
        return privateKey;
    }

    private PublicKey getPublicKey() {
        if (publicKey == null) {
            synchronized (this) {
                if (publicKey == null) {
                    if (publicKeyLocation == null || publicKeyLocation.isBlank())
                        throw new IllegalStateException(
                            "app.jwt.algorithm=RS256 but app.jwt.public-key is not configured — cannot verify tokens");
                    try {
                        publicKey = KeyFactory.getInstance("RSA")
                                .generatePublic(new X509EncodedKeySpec(readPem(publicKeyLocation)));
                        log.info("JWT: loaded RSA public key from {}", publicKeyLocation);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                            "Failed to load JWT public key from " + publicKeyLocation +
                            " (expecting X.509 PEM): " + e.getMessage(), e);
                    }
                }
            }
        }
        return publicKey;
    }

    private javax.crypto.SecretKey getHmacKey() {
        if (secret == null || secret.isBlank())
            throw new IllegalStateException(
                "app.jwt.algorithm=HS256 but onlyoffice.jwt.secret is not configured");
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Token operations ──────────────────────────────────────────────────────

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry);

        return isRsa()
                ? builder.signWith(getPrivateKey(), SignatureAlgorithm.RS256).compact()
                : builder.signWith(getHmacKey(),    SignatureAlgorithm.HS256).compact();
    }

    public Claims validateAndExtract(String token) {
        var parser = isRsa()
                ? Jwts.parserBuilder().setSigningKey(getPublicKey())
                : Jwts.parserBuilder().setSigningKey(getHmacKey());
        return parser.build().parseClaimsJws(token).getBody();
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
