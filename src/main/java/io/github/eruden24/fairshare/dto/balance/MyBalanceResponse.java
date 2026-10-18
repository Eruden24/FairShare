package io.github.eruden24.fairshare.dto.balance;

import java.math.BigDecimal;
import java.util.UUID;

/** My balance in one of my groups. There's no grand total: groups can use different currencies. */
public record MyBalanceResponse(
        UUID groupId,
        String groupName,
        String currency,
        BigDecimal paid,
        BigDecimal owed,
        BigDecimal balance
) {}
