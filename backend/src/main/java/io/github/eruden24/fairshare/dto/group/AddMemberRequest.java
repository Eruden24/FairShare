package io.github.eruden24.fairshare.dto.group;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
    @NotBlank
    @Email
    String email
) {}
