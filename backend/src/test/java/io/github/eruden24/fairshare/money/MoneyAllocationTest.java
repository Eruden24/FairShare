package io.github.eruden24.fairshare.money;

import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyAllocationTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency JPY = Currency.getInstance("JPY");

    private static Money eur(long minorUnits) {
        return new Money(minorUnits, EUR);
    }

    private static List<Long> minorUnits(List<Money> parts) {
        return parts.stream().map(Money::minorUnits).toList();
    }

    private static long sum(List<Money> parts) {
        return parts.stream().mapToLong(Money::minorUnits).sum();
    }

    @Test
    void splitsEvenlyWhenThereIsNoLeftover() {
        assertThat(minorUnits(eur(900).splitEqually(3, new Random(1)))).containsExactly(300L, 300L, 300L);
    }

    @Test
    void givesTheLeftoverCentToExactlyOnePerson() {
        List<Money> parts = eur(1000).splitEqually(3, new Random(1));

        assertThat(minorUnits(parts)).containsExactlyInAnyOrder(334L, 333L, 333L);
    }

    @Test
    void sameSeedGivesSameResult() {
        List<Money> first = eur(1000).splitEqually(3, new Random(42));
        List<Money> second = eur(1000).splitEqually(3, new Random(42));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void leftoverCentLandsOnEveryoneOverManyRuns() {
        Set<Integer> receivers = new HashSet<>();
        for (int seed = 0; seed < 200; seed++) {
            List<Long> parts = minorUnits(eur(1000).splitEqually(3, new Random(seed)));
            receivers.add(parts.indexOf(334L));
        }

        assertThat(receivers).containsExactlyInAnyOrder(0, 1, 2);
    }

    @Test
    void splitsByPercentages() {
        List<Money> parts = eur(1000).allocate(new long[]{50, 30, 20}, new Random(1));

        assertThat(minorUnits(parts)).containsExactly(500L, 300L, 200L);
    }

    @Test
    void leftoverGoesToTheLargestRemainderNotRandomly() {
        // 1001 by [2, 1, 1] = 500.5 / 250.25 / 250.25: the first part lost the most to rounding
        for (int seed = 0; seed < 50; seed++) {
            List<Money> parts = eur(1001).allocate(new long[]{2, 1, 1}, new Random(seed));

            assertThat(minorUnits(parts)).containsExactly(501L, 250L, 250L);
        }
    }

    @Test
    void handlesFewerCentsThanPeople() {
        List<Money> parts = eur(2).splitEqually(3, new Random(1));

        assertThat(minorUnits(parts)).containsExactlyInAnyOrder(1L, 1L, 0L);
    }

    @Test
    void splitsNegativeAmountsLikePositiveOnes() {
        List<Money> parts = eur(-1000).splitEqually(3, new Random(1));

        assertThat(minorUnits(parts)).containsExactlyInAnyOrder(-334L, -333L, -333L);
    }

    @Test
    void keepsTheCurrency() {
        List<Money> parts = new Money(1000, JPY).splitEqually(3, new Random(1));

        assertThat(parts).allSatisfy(part -> assertThat(part.currency()).isEqualTo(JPY));
        assertThat(minorUnits(parts)).containsExactlyInAnyOrder(334L, 333L, 333L);
    }

    @Test
    void partsAlwaysAddUpToTheTotal() {
        Random random = new Random(7);
        for (int run = 0; run < 1000; run++) {
            long total = random.nextLong(-1_000_000, 1_000_000);
            long[] weights = random.longs(random.nextInt(1, 10), 1, 100).toArray();

            List<Money> parts = eur(total).allocate(weights, random);

            assertThat(parts).hasSize(weights.length);
            assertThat(sum(parts)).as("total %d, weights %s", total, weights).isEqualTo(total);
        }
    }

    @Test
    void rejectsInvalidWeights() {
        Random random = new Random(1);

        assertThatThrownBy(() -> eur(1000).allocate(new long[]{}, random))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> eur(1000).allocate(new long[]{1, 0}, random))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> eur(1000).allocate(new long[]{1, -1}, random))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> eur(1000).splitEqually(0, random))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
