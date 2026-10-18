package io.github.eruden24.fairshare.split;

import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.random.RandomGenerator;

/** Each value is that participant's amount, e.g. 7.50 + 2.50 for a 10.00 expense. No rounding involved. */
class ExactSplit implements SplitStrategy {

    @Override
    public List<Money> split(Money total, List<BigDecimal> values, RandomGenerator random) {
        SplitValues.requireAll(values, "their exact amount");

        List<Money> shares = values.stream().map(value -> toShare(value, total)).toList();

        Money sum = shares.stream().reduce(Money.zero(total.currency()), Money::plus);
        if (!sum.equals(total)) {
            throw new InvalidRequestException(SplitValues.FIELD,
                    "exact amounts add up to " + sum + " but the expense is " + total);
        }
        return shares;
    }

    private static Money toShare(BigDecimal value, Money total) {
        if (value.signum() < 0) {
            throw new InvalidRequestException(SplitValues.FIELD, "exact amounts can't be negative");
        }
        try {
            return Money.of(value, total.currency());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(SplitValues.FIELD, "exact amounts can have at most "
                    + total.currency().getDefaultFractionDigits() + " decimals in " + total.currency());
        }
    }
}
