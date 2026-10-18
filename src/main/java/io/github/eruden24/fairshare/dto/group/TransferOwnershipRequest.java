package io.github.eruden24.fairshare.dto.group;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferOwnershipRequest(
    @NotNull
    UUID userId
) {}
