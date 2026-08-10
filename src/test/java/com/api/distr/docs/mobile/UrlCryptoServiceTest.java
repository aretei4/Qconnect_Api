package com.api.distr.docs.mobile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UrlCryptoService")
class UrlCryptoServiceTest {

    private UrlCryptoService service() {
        UrlCryptoService s = new UrlCryptoService();
        ReflectionTestUtils.setField(s, "secret", "abq3EH95CcmsYqrz5voHdGJkxCRqm0fE");
        return s;
    }

    @Test
    @DisplayName("encrypt → decrypt round trip returns the original payload")
    void roundTrip() {
        UrlCryptoService s = service();
        String plain = "45|1|1784289600000";
        String token = s.encrypt(plain);

        assertThat(token).doesNotContain("|");           // opaque
        assertThat(token).matches("[A-Za-z0-9_-]+");     // URL-safe base64
        assertThat(s.decrypt(token)).isEqualTo(plain);
    }

    @Test
    @DisplayName("same payload encrypts to different tokens (random IV)")
    void randomised() {
        UrlCryptoService s = service();
        assertThat(s.encrypt("45|1|0")).isNotEqualTo(s.encrypt("45|1|0"));
    }

    @Test
    @DisplayName("tampered token is rejected")
    void tamperRejected() {
        UrlCryptoService s = service();
        String token = s.encrypt("45|1|0");
        String tampered = token.substring(0, token.length() - 2) + "AA";
        assertThatThrownBy(() -> s.decrypt(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("token from a different secret is rejected")
    void wrongSecretRejected() {
        String token = service().encrypt("45|1|0");
        UrlCryptoService other = new UrlCryptoService();
        ReflectionTestUtils.setField(other, "secret", "another-secret");
        assertThatThrownBy(() -> other.decrypt(token))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
