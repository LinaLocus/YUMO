package com.voicenotes.service;

import com.voicenotes.domain.User;
import com.voicenotes.dto.AuthDtos.AuthResponse;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.UserRepository;
import com.voicenotes.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(String username, String password) {
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        users.save(u);
        return new AuthResponse(jwt.generateToken(u.getUsername(), u.getId()), u.getUsername());
    }

    public AuthResponse login(String username, String password) {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new AuthResponse(jwt.generateToken(u.getUsername(), u.getId()), u.getUsername());
    }
}
