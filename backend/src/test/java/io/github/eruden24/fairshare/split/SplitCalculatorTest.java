package io.github.eruden24.fairshare.split;

import io.github.eruden24.fairshare.enumeration.SplitType;
import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.money.Money;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * One nested class per split type. Values are written as strings ("33.33") and null for "no value".
 */
class SplitCalculatorTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private final SplitCalculator calculator = new SplitCalculator(new Random(42));

    private List<Long> split(SplitType type, long totalMinor, String... values) {
        List<BigDecimal> parsed = Arrays.stream(values).map(v -> v == null ? null : new BigDecimal(v)).toList();
        return calculator.split(type, new Money(totalMinor, EUR), parsed).stream().map(Money::minorUnits).toList();
    }

    private void assertRejected(SplitType type, long totalMinor, String expectedMessage, String... values) {
        assertThatThrownBy(() -> split(type, totalMinor, values))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(expectedMessage)
                .extracting("field").isEqualTo("participants");
    }

    @Nested
    class Equal {

        @Test
        void splitsEvenlyWithTheLeftoverCentOnOnePerson() {
            assertThat(split(SplitType.EQUAL, 1000, null, null, null)).containsExactlyInAnyOrder(334L, 333L, 333L);
        }

        @Test
        void rejectsValues() {
            assertRejected(SplitType.EQUAL, 1000, "not used in an EQUAL split", "5", null);
        }
    }

    @Nested
    class Exact {

        @Test
        void usesTheAmountsAsGiven() {
            assertThat(split(SplitType.EXACT, 1000, "7.50", "2.50")).containsExactly(750L, 250L);
        }

        @Test
        void allowsAZeroShare() {
            assertThat(split(SplitType.EXACT, 1000, "10", "0")).containsExactly(1000L, 0L);
        }

        @Test
        void rejectsAmountsThatDoNotAddUp() {
            assertRejected(SplitType.EXACT, 1000, "add up to 9.00 EUR but the expense is 10.00 EUR", "7.00", "2.00");
        }

        @Test
        void rejectsMissingNegativeOrTooPreciseAmounts() {
            assertRejected(SplitType.EXACT, 1000, "every participant needs a value", "10", null);
            assertRejected(SplitType.EXACT, 1000, "can't be negative", "11", "-1");
            assertRejected(SplitType.EXACT, 1000, "at most 2 decimals", "9.995", "0.005");
        }
    }

    @Nested
    class Percentage {

        @Test
        void splitsByPercentage() {
            assertThat(split(SplitType.PERCENTAGE, 1000, "50", "30", "20")).containsExactly(500L, 300L, 200L);
        }

        @Test
        void acceptsTwoDecimalPercentagesAndKeepsTheTotal() {
            List<Long> shares = split(SplitType.PERCENTAGE, 1000, "33.33", "33.33", "33.34");

            assertThat(shares.stream().mapToLong(Long::longValue).sum()).isEqualTo(1000);
            assertThat(shares).containsExactly(333L, 333L, 334L);
        }

        @Test
        void rejectsPercentagesThatDoNotAddUpTo100() {
            assertRejected(SplitType.PERCENTAGE, 1000, "add up to 90 but must add up to 100", "50", "40");
            assertRejected(SplitType.PERCENTAGE, 1000, "add up to 100.01", "50", "50.01");
        }

        @Test
        void rejectsZeroOrTooPrecisePercentages() {
            assertRejected(SplitType.PERCENTAGE, 1000, "must be positive", "100", "0");
            assertRejected(SplitType.PERCENTAGE, 1000, "at most 2 decimals", "33.333", "66.667");
        }
    }

    @Nested
    class Shares {

        @Test
        void splitsByShares() {
            // 2 nights vs 1 night each: the first person pays half
            assertThat(split(SplitType.SHARES, 1000, "2", "1", "1")).containsExactly(500L, 250L, 250L);
        }

        @Test
        void acceptsFractionalShares() {
            assertThat(split(SplitType.SHARES, 1000, "1.5", "0.5")).containsExactly(750L, 250L);
        }

        @Test
        void rejectsMissingOrZeroShares() {
            assertRejected(SplitType.SHARES, 1000, "every participant needs a value", "1", null);
            assertRejected(SplitType.SHARES, 1000, "must be positive", "1", "0");
        }
    }
}
