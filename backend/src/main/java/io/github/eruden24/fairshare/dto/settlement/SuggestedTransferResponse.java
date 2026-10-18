package io.github.eruden24.fairshare.dto.settlement;

import java.math.BigDecimal;
import java.util.UUID;

/** "from should pay amount to to". Recording it as a settlement brings both closer to zero. */
public record SuggestedTransferResponse(
        UUID fromUserId,
        String fromDisplayName,
        UUID toUserId,
        String toDisplayName,
        BigDecimal amount
) {}
