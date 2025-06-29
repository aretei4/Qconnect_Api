package com.api.qualcy.docs.onlyoffice;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoder;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${onlyoffice.jwt.secret}")
    private  String secret;//= "abq3EH95CcmsYqrz5voHdGJkxCRqm0fE";//"fJ2lTiubmBQwXIrG9NfLFGEHbeQmwYsl";

    public String sign(Map<String, Object> payload) {
    	// Set expiration time (e.g., 1 hour from now)
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (60 * 60 * 1000); // 1 hour
        Date exp = new Date(expMillis);
    	           
        return Jwts.builder()
        		.setExpiration(exp)
        		.setIssuedAt(new Date(nowMillis))
                .setClaims(payload)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
    public  Map<String, Object> verify(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token).getBody();
    }
}

