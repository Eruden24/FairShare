package io.github.eruden24.fairshare.split;

import io.github.eruden24.fairshare.enumeration.SplitType;
import io.github.eruden24.fairshare.money.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Picks the strategy for a split type. The only public entry point to the split package.
 */
@Component
public class SplitCalculator {

    private final Map<SplitType, SplitStrategy> strategies = new EnumMap<>(Map.of(
            SplitType.EQUAL, new EqualSplit(),
            SplitType.EXACT, new ExactSplit(),
            SplitType.PERCENTAGE, new PercentageSplit(),
            SplitType.SHARES, new SharesSplit()
    ));

    private final RandomGenerator splitRandom;

    public SplitCalculator(RandomGenerator splitRandom) {
        this.splitRandom = splitRandom;
    }

    /** @return one share per value, in the same order, adding up exactly to total */
    public List<Money> split(SplitType type, Money total, List<BigDecimal> values) {
        return strategies.get(type).split(total, values, splitRandom);
    }
}
