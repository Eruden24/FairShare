package io.github.eruden24.fairshare.split;

import io.github.eruden24.fairshare.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Turns an expense total and one value per participant into each participant's share.
 * What a value means (nothing, an amount, a percentage, a number of shares) depends on the strategy.
 * Invalid values are reported as an InvalidRequestException on the "participants" field.
 */
public interface SplitStrategy {

    /** @return the shares, in the same order as the values, adding up exactly to total */
    List<Money> split(Money total, List<BigDecimal> values, RandomGenerator random);
}
