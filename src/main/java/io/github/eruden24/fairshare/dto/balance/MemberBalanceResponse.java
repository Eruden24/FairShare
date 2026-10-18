package io.github.eruden24.fairshare.dto.balance;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * balance = paid - owed. Positive: the group owes this person. Negative: they owe the group.
 * member is false for someone who left the group but still has expenses in it.
 */
public record MemberBalanceResponse(
        UUID userId,
        String displayName,
        boolean member,
        BigDecimal paid,
        BigDecimal owed,
        BigDecimal balance
) {}
