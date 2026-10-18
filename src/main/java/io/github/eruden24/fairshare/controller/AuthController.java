package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.dto.auth.LoginRequest;
import io.github.eruden24.fairshare.dto.auth.TokenResponse;
import io.github.eruden24.fairshare.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
