package io.github.eruden24.fairshare.split;

import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.random.RandomGenerator;

/** 10.00 between 3 people: 3.34 / 3.33 / 3.33, the extra cent going to a random participant. */
class EqualSplit implements SplitStrategy {

    @Override
    public List<Money> split(Money total, List<BigDecimal> values, RandomGenerator random) {
        if (values.stream().anyMatch(value -> value != null)) {
            throw new InvalidRequestException(SplitValues.FIELD, "values are not used in an EQUAL split; leave them out");
        }
        return total.splitEqually(values.size(), random);
    }
}
