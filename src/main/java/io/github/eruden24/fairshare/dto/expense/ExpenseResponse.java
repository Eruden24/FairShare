package io.github.eruden24.fairshare.dto.expense;

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
        UUID createdById,
        Instant createdAt,
        List<SplitResponse> splits
) {}
