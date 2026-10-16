package io.github.eruden24.fairshare.dto.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse (
   UUID id,
   String email,
   String displayName,
   String defaultCurrency,
   Instant createdAt
) {}