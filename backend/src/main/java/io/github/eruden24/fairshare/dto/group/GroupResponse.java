package io.github.eruden24.fairshare.dto.group;

import io.github.eruden24.fairshare.enumeration.GroupRole;

import java.time.Instant;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String currency,
        UUID createdById,
        Instant createdAt,
        long memberCount,
        GroupRole myRole
) {}
