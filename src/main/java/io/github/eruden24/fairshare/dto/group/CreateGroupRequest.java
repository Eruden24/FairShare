package io.github.eruden24.fairshare.dto.group;

import io.github.eruden24.fairshare.validation.ValidCurrency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
    @NotBlank
    @Size(max=100)
    String name,

    @NotBlank
    @ValidCurrency
    String currency
) {}
