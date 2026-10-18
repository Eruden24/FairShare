package io.github.eruden24.fairshare.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * amount is the participant's share. value is what was entered for PERCENTAGE (33.33)
 * and SHARES (2) splits, and null for EQUAL and EXACT.
 */
public record SplitResponse(
        UUID userId,
        String displayName,
        BigDecimal amount,
        @Schema(types = {"number", "null"})
        BigDecimal value
) {}
