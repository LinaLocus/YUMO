package com.voicenotes.service;

import com.voicenotes.domain.User;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.UserRepository;
import com.voicenotes.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    UserRepository users;
    PasswordEncoder encoder;
    JwtService jwt;
    AuthService service;

    @BeforeEach
    void setup() {
        users = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        jwt = mock(JwtService.class);
        when(jwt.generateToken(anyString(), any())).thenReturn("tok");
        service = new AuthService(users, encoder, jwt);
    }

    @Test
    void registerRejectsDuplicate() {
        when(users.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() -> service.register("alice", "secret123"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void registerHashesPassword() {
        when(users.existsByUsername("bob")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0); u.setId(1L); return u;
        });
        var resp = service.register("bob", "secret123");
        assertThat(resp.token()).isEqualTo("tok");
        verify(users).save(argThat(u -> !u.getPasswordHash().equals("secret123")));
    }

    @Test
    void loginRejectsBadPassword() {
        User u = new User();
        u.setId(1L); u.setUsername("bob");
        u.setPasswordHash(encoder.encode("secret123"));
        when(users.findByUsername("bob")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.login("bob", "wrong"))
                .isInstanceOf(ApiException.class);
    }
}
