package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.config.JwtProperties;
import io.github.eruden24.fairshare.dto.auth.LoginRequest;
import io.github.eruden24.fairshare.dto.auth.TokenResponse;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.exception.UnauthorizedException;
import io.github.eruden24.fairshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ISSUER = "fairshare";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final LoginAttemptLimiter loginAttemptLimiter;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = UserService.normalizeEmail(request.email());
        loginAttemptLimiter.checkAllowed(email);

        // Same message for unknown email and wrong password, so callers can't probe which emails exist
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> {
                    loginAttemptLimiter.recordFailure(email);
                    return new UnauthorizedException("Invalid email or password");
                });
        loginAttemptLimiter.recordSuccess(email);

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.expiration()))
                .subject(user.getId().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", jwtProperties.expiration().toSeconds());
    }
}
