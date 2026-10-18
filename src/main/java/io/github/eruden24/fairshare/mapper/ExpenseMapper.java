package io.github.eruden24.fairshare.mapper;

import io.github.eruden24.fairshare.dto.expense.ExpenseResponse;
import io.github.eruden24.fairshare.dto.expense.SplitResponse;
import io.github.eruden24.fairshare.entity.Expense;
import io.github.eruden24.fairshare.entity.ExpenseSplit;
import io.github.eruden24.fairshare.money.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.Currency;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target="groupId", source="group.id")
    @Mapping(target="amount", expression="java(toDecimal(expense.getAmountMinor(), expense.getGroup().getCurrency()))")
    @Mapping(target="currency", source="group.currency")
    @Mapping(target="paidById", source="paidBy.id")
    @Mapping(target="date", source="expenseDate")
    @Mapping(target="createdById", source="createdBy.id")
    ExpenseResponse toResponse(Expense expense);

    @Mapping(target="userId", source="user.id")
    @Mapping(target="displayName", source="user.displayName")
    @Mapping(target="amount", expression="java(toDecimal(split.getAmountMinor(), split.getExpense().getGroup().getCurrency()))")
    SplitResponse toSplitResponse(ExpenseSplit split);

    // Minor units -> decimal for the API: 1250 EUR -> 12.50
    default BigDecimal toDecimal(long minorUnits, String currency) {
        return new Money(minorUnits, Currency.getInstance(currency)).toDecimal();
    }
}
