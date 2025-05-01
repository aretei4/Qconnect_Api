package com.qualcy.account.security;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class SecurityUtil {

	public static byte[] signData(String salt) throws Exception {
	     PrivateKey privateKey = loadPrivateKey("private_key.pem");
		Signature signer = Signature.getInstance("SHA256withRSA");
		signer.initSign(privateKey);
		signer.update(salt.getBytes(StandardCharsets.UTF_8));
		return signer.sign();
	}

	public static String generateSalt() {
		byte[] salt = new byte[16]; // 16 bytes salt
		new SecureRandom().nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt);
	}

	private static PrivateKey loadPrivateKey(String resourcePath) throws Exception {
		InputStream is = SecurityUtil.class.getClassLoader().getResourceAsStream(resourcePath);
		if (is == null) {
			throw new FileNotFoundException("Resource not found: " + resourcePath);
		}
		String key = new String(is.readAllBytes()).replaceAll("-----BEGIN (.*)-----", "")
				.replaceAll("-----END (.*)-----", "").replaceAll("\\s", "");

		byte[] keyBytes = Base64.getDecoder().decode(key);

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	private static PublicKey loadPublicKey(String resourcePath) throws Exception {
		InputStream is = SecurityUtil.class.getClassLoader().getResourceAsStream(resourcePath);
		if (is == null) {
			throw new FileNotFoundException("Resource not found: " + resourcePath);
		}
		String key = new String(is.readAllBytes()).replaceAll("-----BEGIN (.*)-----", "")
				.replaceAll("-----END (.*)-----", "").replaceAll("\\s", "");

		byte[] keyBytes = Base64.getDecoder().decode(key);

		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}
	
	 public static boolean verifySignature(String salt, String signature) throws Exception {
		 byte[] signatureBytes = Base64.getDecoder().decode(signature);
		 PublicKey publicKey = loadPublicKey("public_key.pem");
	        Signature verifier = Signature.getInstance("SHA256withRSA");
	        verifier.initVerify(publicKey);
	        verifier.update(salt.getBytes(StandardCharsets.UTF_8));
	        return verifier.verify(signatureBytes);
	    }

}
