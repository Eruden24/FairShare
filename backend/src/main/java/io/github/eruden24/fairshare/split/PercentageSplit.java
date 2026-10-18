package io.github.eruden24.fairshare.split;

import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.money.Money;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

/** Each value is a percentage, e.g. 50 / 30 / 20. They must add up to exactly 100. */
class PercentageSplit implements SplitStrategy {

    // 100 % as a weight with 2 decimals (see SplitValues.toWeights)
    private static final long HUNDRED_PERCENT = 10_000;

    @Override
    public List<Money> split(Money total, List<BigDecimal> values, RandomGenerator random) {
        long[] weights = SplitValues.toWeights(values, "a percentage");

        long sum = Arrays.stream(weights).sum();
        if (sum != HUNDRED_PERCENT) {
            throw new InvalidRequestException(SplitValues.FIELD,
                    "percentages add up to " + BigDecimal.valueOf(sum, 2).stripTrailingZeros().toPlainString()
                            + " but must add up to 100");
        }
        return total.allocate(weights, random);
    }
}
