package com.voicenotes.security;

import com.voicenotes.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    JwtService jwt;

    @BeforeEach
    void setup() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("test-secret-test-secret-test-secret-test-secret-1234");
        props.getJwt().setExpirationMs(3600000);
        jwt = new JwtService(props);
    }

    @Test
    void roundTripSubject() {
        String token = jwt.generateToken("alice", 42L);
        assertThat(jwt.extractUsername(token)).isEqualTo("alice");
        assertThat(jwt.extractUserId(token)).isEqualTo(42L);
        assertThat(jwt.isValid(token, "alice")).isTrue();
    }

    @Test
    void invalidTokenRejected() {
        assertThat(jwt.isValid("garbage.token.value", "alice")).isFalse();
    }
}
