package io.github.eruden24.fairshare.dto.user;

import io.github.eruden24.fairshare.validation.ValidCurrency;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update: fields left null are not changed.
 */
public record UpdateUserRequest(
    @Size(max=100)
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    String displayName,

    @ValidCurrency
    String defaultCurrency
) {}
