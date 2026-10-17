package io.github.eruden24.fairshare.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
    @NotBlank
    @Size(max=100)
    String name,

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter currency code like EUR")
    String currency
) {}
