package com.qualcy.account.security;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class QualcyJwtUtil {
	
	static String secret = "my-secret-key-1234567890-ABCDEFGHIJKL";//System.getenv("JWT_SECRET");
	private static final Key SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes());
	 private static final long VALIDITY = 5 * 60 * 1000;
	
	 public static final String SIGN_SALT = "salt";
	 public static final String SIGN_TOKEN = "signToken";
	 
	  public static String buildJwt(Map<String, Object> claims) throws Exception {
		  String salt = SecurityUtil.generateSalt();
		  byte[] signatureBytes = SecurityUtil.signData(salt);
     	 String signToken = Base64.getEncoder().encodeToString(signatureBytes);
     	claims.put(SIGN_TOKEN, signToken);
     	claims.put(SIGN_SALT, salt);
	        return Jwts.builder()
	                .subject("user@example.com")
	                .claims(claims) // Add all HashMap claims at once
	                .issuedAt(new Date())
	                .expiration(new Date(System.currentTimeMillis() + VALIDITY)) // 1 hour
	                .signWith(SECRET_KEY)
	                .compact();
	    }
	 
	    public static Map<String, Object> parseJwt(String token) {
	        try {
	            Jws<Claims> jws = Jwts.parser()
	                    .setSigningKey(SECRET_KEY)
	                    .build()
	                    .parseSignedClaims(token);

	            // Convert Claims to HashMap
	            return new HashMap<>(jws.getPayload());
	        } catch (JwtException e) {
	            throw new RuntimeException("JWT validation failed", e);
	        }
	    }
}
