package io.github.eruden24.fairshare.dto.auth;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
