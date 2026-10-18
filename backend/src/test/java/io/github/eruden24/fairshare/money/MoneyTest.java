package io.github.eruden24.fairshare.money;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @ParameterizedTest
    @CsvSource({
            "12.50,  EUR, 1250",
            "12.5,   EUR, 1250",
            "12.500, EUR, 1250",
            "12,     EUR, 1200",
            "0.01,   EUR, 1",
            "-3.20,  EUR, -320",
            "1000,   JPY, 1000",
            "1.234,  KWD, 1234"
    })
    void convertsDecimalToMinorUnits(String amount, String currency, long expectedMinorUnits) {
        Money money = Money.of(new BigDecimal(amount), Currency.getInstance(currency));

        assertThat(money.minorUnits()).isEqualTo(expectedMinorUnits);
    }

    @ParameterizedTest
    @CsvSource({
            "12.505, EUR",
            "0.001,  EUR",
            "1000.5, JPY",
            "1.2345, KWD"
    })
    void rejectsMoreDecimalsThanTheCurrencyHas(String amount, String currency) {
        assertThatThrownBy(() -> Money.of(new BigDecimal(amount), Currency.getInstance(currency)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a valid amount in " + currency);
    }

    @Test
    void rejectsAmountsThatDoNotFitInALong() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("100000000000000000000"), EUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCurrenciesWithoutMinorUnits() {
        assertThatThrownBy(() -> new Money(100, Currency.getInstance("XAU")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "1250, EUR, 12.50",
            "5,    EUR, 0.05",
            "-320, EUR, -3.20",
            "1000, JPY, 1000",
            "1234, KWD, 1.234"
    })
    void convertsMinorUnitsToDecimal(long minorUnits, String currency, String expected) {
        Money money = new Money(minorUnits, Currency.getInstance(currency));

        assertThat(money.toDecimal().toPlainString()).isEqualTo(expected);
    }

    @Test
    void addsAndSubtracts() {
        Money a = new Money(1250, EUR);
        Money b = new Money(300, EUR);

        assertThat(a.plus(b)).isEqualTo(new Money(1550, EUR));
        assertThat(a.minus(b)).isEqualTo(new Money(950, EUR));
        assertThat(b.minus(a)).isEqualTo(new Money(-950, EUR));
        assertThat(a.negate()).isEqualTo(new Money(-1250, EUR));
    }

    @Test
    void rejectsMixingCurrencies() {
        assertThatThrownBy(() -> new Money(100, EUR).plus(new Money(100, USD)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void throwsOnOverflowInsteadOfWrappingAround() {
        assertThatThrownBy(() -> new Money(Long.MAX_VALUE, EUR).plus(new Money(1, EUR)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void reportsSign() {
        assertThat(new Money(1, EUR).isPositive()).isTrue();
        assertThat(new Money(-1, EUR).isNegative()).isTrue();
        assertThat(Money.zero(EUR).isZero()).isTrue();
    }

    @Test
    void printsAmountAndCode() {
        assertThat(new Money(1250, EUR)).hasToString("12.50 EUR");
    }
}
