package com.api.distr.docs.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserJwtService")
class UserJwtServiceTest {

    private UserJwtService service(String algorithm) {
        UserJwtService s = new UserJwtService();
        ReflectionTestUtils.setField(s, "algorithm", algorithm);
        ReflectionTestUtils.setField(s, "secret", "abq3EH95CcmsYqrz5voHdGJkxCRqm0fE");
        ReflectionTestUtils.setField(s, "privateKeyLocation", "keys/jwt-private.pem");
        ReflectionTestUtils.setField(s, "publicKeyLocation",  "keys/jwt-public.pem");
        ReflectionTestUtils.setField(s, "expirationMs", 60_000L);
        return s;
    }

    @Test
    @DisplayName("HS256 round trip — sign with secret, validate, extract claims")
    void hs256RoundTrip() {
        UserJwtService s = service("HS256");
        String token = s.generateToken("admin", "ROLE_ADMIN");
        assertThat(s.isTokenValid(token)).isTrue();
        assertThat(s.extractUsername(token)).isEqualTo("admin");
        assertThat(s.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("RS256 round trip — sign with private key, verify with public key")
    void rs256RoundTrip() {
        UserJwtService s = service("RS256");
        String token = s.generateToken("manager", "ROLE_MANAGER");
        assertThat(s.isTokenValid(token)).isTrue();
        assertThat(s.extractUsername(token)).isEqualTo("manager");
        assertThat(s.extractRole(token)).isEqualTo("ROLE_MANAGER");
    }

    @Test
    @DisplayName("HS256 token is rejected when validated as RS256")
    void hsTokenRejectedByRsa() {
        String hsToken = service("HS256").generateToken("admin", "ROLE_ADMIN");
        assertThat(service("RS256").isTokenValid(hsToken)).isFalse();
    }

    @Test
    @DisplayName("RS256 keys load from the bundled classpath (production config)")
    void rs256LoadsFromClasspath() {
        UserJwtService s = new UserJwtService();
        ReflectionTestUtils.setField(s, "algorithm", "RS256");
        ReflectionTestUtils.setField(s, "privateKeyLocation", "classpath:keys/jwt-private.pem");
        ReflectionTestUtils.setField(s, "publicKeyLocation",  "classpath:keys/jwt-public.pem");
        ReflectionTestUtils.setField(s, "expirationMs", 60_000L);

        String token = s.generateToken("admin", "ROLE_ADMIN");
        assertThat(s.isTokenValid(token)).isTrue();
        assertThat(s.extractUsername(token)).isEqualTo("admin");
    }
}
