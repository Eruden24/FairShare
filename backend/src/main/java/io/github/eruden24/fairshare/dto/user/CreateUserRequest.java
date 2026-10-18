package io.github.eruden24.fairshare.dto.user;

import io.github.eruden24.fairshare.validation.ValidCurrency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest (

    @NotBlank
    @Email
    @Size(max=255)
    String email,

    @NotBlank
    @Size(min=8, max=72)
    String password,

    @NotBlank
    @Size(max=100)
    String displayName,

    @ValidCurrency
    String defaultCurrency
) {}

