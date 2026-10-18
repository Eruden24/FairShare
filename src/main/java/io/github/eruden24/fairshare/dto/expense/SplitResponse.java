package io.github.eruden24.fairshare.dto.expense;

import java.math.BigDecimal;
import java.util.UUID;

public record SplitResponse(
        UUID userId,
        String displayName,
        BigDecimal amount
) {}
