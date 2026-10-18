package io.github.eruden24.fairshare.dto.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID groupId,
        UUID fromUserId,
        String fromDisplayName,
        UUID toUserId,
        String toDisplayName,
        BigDecimal amount,
        String currency,
        LocalDate date,
        UUID createdById,
        Instant createdAt
) {}
