package io.github.eruden24.fairshare.dto.settlement;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * fromUserId paid amount to toUserId, in the group's currency. Both must be members of the group.
 */
public record SettlementRequest(
    @NotNull
    UUID fromUserId,

    @NotNull
    UUID toUserId,

    @NotNull
    @Positive
    @Digits(integer=12, fraction=3)
    BigDecimal amount,

    // Optional: defaults to today
    LocalDate date
) {}
