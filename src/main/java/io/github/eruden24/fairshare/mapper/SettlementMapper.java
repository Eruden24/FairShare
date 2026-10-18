package io.github.eruden24.fairshare.mapper;

import io.github.eruden24.fairshare.dto.settlement.SettlementResponse;
import io.github.eruden24.fairshare.entity.Settlement;
import io.github.eruden24.fairshare.money.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SettlementMapper {

    @Mapping(target="groupId", source="group.id")
    @Mapping(target="fromUserId", source="fromUser.id")
    @Mapping(target="fromDisplayName", source="fromUser.displayName")
    @Mapping(target="toUserId", source="toUser.id")
    @Mapping(target="toDisplayName", source="toUser.displayName")
    @Mapping(target="amount", expression="java(toDecimal(settlement.getAmountMinor(), settlement.getGroup().getCurrency()))")
    @Mapping(target="currency", source="group.currency")
    @Mapping(target="date", source="settledOn")
    @Mapping(target="createdById", source="createdBy.id")
    SettlementResponse toResponse(Settlement settlement);

    List<SettlementResponse> toResponse(List<Settlement> settlements);

    // Minor units -> decimal for the API: 1250 EUR -> 12.50
    default BigDecimal toDecimal(long minorUnits, String currency) {
        return new Money(minorUnits, Currency.getInstance(currency)).toDecimal();
    }
}
