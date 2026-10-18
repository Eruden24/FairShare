package io.github.eruden24.fairshare.split;

import io.github.eruden24.fairshare.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.random.RandomGenerator;

/** Each value is a number of shares, e.g. 2 / 1 / 1: the first person pays half. Any positive numbers work. */
class SharesSplit implements SplitStrategy {

    @Override
    public List<Money> split(Money total, List<BigDecimal> values, RandomGenerator random) {
        return total.allocate(SplitValues.toWeights(values, "a number of shares"), random);
    }
}
