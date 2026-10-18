package io.github.eruden24.fairshare.dto.expense;

import io.github.eruden24.fairshare.enumeration.SplitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID groupId,
        String description,
        BigDecimal amount,
        String currency,
        UUID paidById,
        LocalDate date,
        SplitType splitType,
        UUID createdById,
        Instant createdAt,
        Instant updatedAt,
        List<SplitResponse> splits
) {}
