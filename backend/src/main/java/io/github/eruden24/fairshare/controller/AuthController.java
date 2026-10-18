package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.dto.auth.LoginRequest;
import io.github.eruden24.fairshare.dto.auth.TokenResponse;
import io.github.eruden24.fairshare.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Log in to get a JWT for the other endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Log in and get a Bearer token (5 failed attempts per email in 15 minutes → 429)")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
