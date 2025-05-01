package com.qualcy.account.user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.qualcy.account.security.QualcyJwtUtil;
import com.qualcy.account.security.SecurityUtil;

public class JwtExample {

	public static void main(String[] args) {
		// Create a HashMap for custom claims
		Map<String, Object> customClaims = new HashMap<>();
		customClaims.put("userId", 12345);
		customClaims.put("roles", Arrays.asList("ADMIN", "USER"));
		customClaims.put("isActive", true);

		// Build JWT with HashMap claims

		// System.out.println("Generated JWT:\n" + token);
		try {
			String token = QualcyJwtUtil.buildJwt(customClaims);
			System.out.println("Generated JWT:\n" + token);
			Map<String, Object> parsedClaims = QualcyJwtUtil.parseJwt(token);
			System.out.println("\nParsed Claims:\n" + parsedClaims);
			String signToken = (String) parsedClaims.get(QualcyJwtUtil.SIGN_TOKEN);
			String signSalt = (String) parsedClaims.get(QualcyJwtUtil.SIGN_SALT);
			System.out.println("Signature (Base64): " + signToken);
			System.out.println(" Is certificate valid : -   " + SecurityUtil.verifySignature(signSalt, signToken));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Parse JWT and extract claims

	}

}
