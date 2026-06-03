package com.voicenotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32) String username,
            @NotBlank @Size(min = 6, max = 72) String password) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record AuthResponse(String token, String username) {}
}
