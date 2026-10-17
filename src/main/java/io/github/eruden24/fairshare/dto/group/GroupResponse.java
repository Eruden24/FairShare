package io.github.eruden24.fairshare.dto.group;

import java.time.Instant;
import java.util.UUID;

public record CreateGroupResponse(
        UUID id,
        String name,
        String currency,
        UUID createdById,
        Instant createAt
) {}
