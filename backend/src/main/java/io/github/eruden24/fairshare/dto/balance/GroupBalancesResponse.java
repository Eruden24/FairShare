package io.github.eruden24.fairshare.dto.balance;

import java.util.List;
import java.util.UUID;

/** Every balance in a group. They always add up to zero: what some are owed, others owe. */
public record GroupBalancesResponse(
        UUID groupId,
        String currency,
        List<MemberBalanceResponse> balances
) {}
