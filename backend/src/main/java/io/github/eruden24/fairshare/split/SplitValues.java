package io.github.eruden24.fairshare.split;

import io.github.eruden24.fairshare.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checks shared by the strategies. Errors are reported on "participants", the request field holding the values.
 */
final class SplitValues {

    static final String FIELD = "participants";

    // Percentages and shares may have up to 2 decimals: 33.33 % or 1.5 shares
    private static final int WEIGHT_DECIMALS = 2;

    private SplitValues() {
    }

    static void requireAll(List<BigDecimal> values, String what) {
        if (values.stream().anyMatch(value -> value == null)) {
            throw new InvalidRequestException(FIELD, "every participant needs a value (" + what + ")");
        }
    }

    /**
     * Converts percentages or shares to whole-number weights for Money.allocate: 33.33 -> 3333, 2 -> 200.
     */
    static long[] toWeights(List<BigDecimal> values, String what) {
        requireAll(values, what);
        long[] weights = new long[values.size()];
        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = values.get(i);
            if (value.signum() <= 0) {
                throw new InvalidRequestException(FIELD, what + " must be positive, got " + value.toPlainString());
            }
            try {
                weights[i] = value.movePointRight(WEIGHT_DECIMALS).longValueExact();
            } catch (ArithmeticException e) {
                throw new InvalidRequestException(FIELD,
                        what + " can have at most " + WEIGHT_DECIMALS + " decimals, got " + value.toPlainString());
            }
        }
        return weights;
    }
}
