package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.money.Money;

import java.math.BigDecimal;
import java.util.Currency;

/** Turns an amount from a request into Money in the group's currency, as a 400 on "amount" if it doesn't fit. */
final class Amounts {

    private Amounts() {
    }

    static Money toMoney(BigDecimal amount, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        try {
            return Money.of(amount, currency);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("amount",
                    "must have at most " + currency.getDefaultFractionDigits() + " decimals in " + currencyCode);
        }
    }
}
