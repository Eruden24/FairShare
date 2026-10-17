package io.github.eruden24.fairshare.dto.group;

import io.github.eruden24.fairshare.entity.GroupRole;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
    UUID userId,
    String displayName,
    GroupRole role,
    Instant joinedAt
) {}
