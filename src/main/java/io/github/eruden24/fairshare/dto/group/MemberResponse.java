package io.github.eruden24.fairshare.dto.group;

import io.github.eruden24.fairshare.enumeration.GroupRole;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
    UUID userId,
    String displayName,
    GroupRole role,
    Instant joinedAt
) {}
