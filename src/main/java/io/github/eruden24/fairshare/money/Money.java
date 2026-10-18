package io.github.eruden24.fairshare.money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

/**
 * An amount of money stored as minor units (cents for EUR, yen for JPY, fils for KWD),
 * so all arithmetic is exact integer math. Can be negative, e.g. for balances.
 */
public record Money(long minorUnits, Currency currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        if (currency.getDefaultFractionDigits() < 0) {
            throw new IllegalArgumentException(currency + " has no minor units and can't hold money amounts");
        }
    }

    public static Money of(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        try {
            // 12.50 EUR -> 1250. Throws if a fraction is left (12.505 EUR) or the result doesn't fit in a long.
            long minorUnits = amount.movePointRight(currency.getDefaultFractionDigits()).longValueExact();
            return new Money(minorUnits, currency);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    amount.toPlainString() + " is not a valid amount in " + currency
                            + " (at most " + currency.getDefaultFractionDigits() + " decimals)", e);
        }
    }

    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    /** 1250 EUR -> 12.50, 1000 JPY -> 1000, 1234 KWD -> 1.234 */
    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(minorUnits, currency.getDefaultFractionDigits());
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(minorUnits), currency);
    }

    /**
     * Splits this amount into equal parts that add up exactly to it. Leftover minor units
     * (10.00 / 3 leaves 1 cent) go to randomly chosen parts.
     */
    public List<Money> splitEqually(int parts, RandomGenerator random) {
        if (parts <= 0) {
            throw new IllegalArgumentException("parts must be positive, got " + parts);
        }
        long[] weights = new long[parts];
        Arrays.fill(weights, 1);
        return allocate(weights, random);
    }

    /**
     * Splits this amount in proportion to the weights, e.g. [50, 30, 20] for percentages or [2, 1, 1]
     * for shares. The parts are returned in the same order as the weights and always add up exactly
     * to this amount.
     * <p>
     * Largest remainder method: each part first gets its exact share rounded down, then the leftover
     * minor units go one by one to the parts that lost the most to rounding. Ties are broken randomly,
     * so in an equal split the extra cent lands on a random part.
     */
    public List<Money> allocate(long[] weights, RandomGenerator random) {
        Objects.requireNonNull(weights, "weights");
        Objects.requireNonNull(random, "random");
        if (weights.length == 0) {
            throw new IllegalArgumentException("weights must not be empty");
        }
        long totalWeight = 0;
        for (long weight : weights) {
            if (weight <= 0) {
                throw new IllegalArgumentException("weights must be positive, got " + weight);
            }
            totalWeight = Math.addExact(totalWeight, weight);
        }

        // Split the absolute amount and put the sign back at the end, so -10.00 splits like 10.00
        long amount = Math.absExact(minorUnits);
        long[] parts = new long[weights.length];
        long[] remainders = new long[weights.length];
        long allocated = 0;
        for (int i = 0; i < weights.length; i++) {
            long share = Math.multiplyExact(amount, weights[i]);
            parts[i] = share / totalWeight;
            remainders[i] = share % totalWeight;
            allocated += parts[i];
        }

        // Shuffle, then stable sort by remainder: equal remainders stay in random order
        List<Integer> order = new ArrayList<>(IntStream.range(0, weights.length).boxed().toList());
        shuffle(order, random);
        order.sort(Comparator.comparingLong((Integer i) -> remainders[i]).reversed());

        long leftover = amount - allocated;
        for (int k = 0; k < leftover; k++) {
            parts[order.get(k)]++;
        }

        long sign = minorUnits < 0 ? -1 : 1;
        return Arrays.stream(parts).mapToObj(part -> new Money(sign * part, currency)).toList();
    }

    public boolean isPositive() {
        return minorUnits > 0;
    }

    public boolean isNegative() {
        return minorUnits < 0;
    }

    public boolean isZero() {
        return minorUnits == 0;
    }

    @Override
    public String toString() {
        return toDecimal().toPlainString() + " " + currency.getCurrencyCode();
    }

    // Fisher-Yates; Collections.shuffle only accepts java.util.Random, not any RandomGenerator
    private static void shuffle(List<Integer> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
