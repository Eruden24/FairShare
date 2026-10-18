package io.github.eruden24.fairshare.dto.balance;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * balance = paid - owed + sent - received. Positive: the group owes this person. Negative: they owe the group.
 * sent / received are settlements paid to / received from other members.
 * member is false for someone who left the group but still has expenses or settlements in it.
 */
public record MemberBalanceResponse(
        UUID userId,
        String displayName,
        boolean member,
        BigDecimal paid,
        BigDecimal owed,
        BigDecimal sent,
        BigDecimal received,
        BigDecimal balance
) {}
