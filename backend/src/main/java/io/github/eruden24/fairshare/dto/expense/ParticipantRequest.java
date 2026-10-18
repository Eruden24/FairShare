package io.github.eruden24.fairshare.dto.expense;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * value depends on the split type: left out for EQUAL, the exact amount for EXACT,
 * a percentage for PERCENTAGE, a number of shares for SHARES.
 */
public record ParticipantRequest(
    @NotNull
    UUID userId,

    @Digits(integer=12, fraction=3)
    BigDecimal value
) {}
