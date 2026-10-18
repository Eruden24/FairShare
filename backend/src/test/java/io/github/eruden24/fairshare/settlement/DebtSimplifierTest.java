package io.github.eruden24.fairshare.settlement;

import io.github.eruden24.fairshare.settlement.DebtSimplifier.Transfer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtSimplifierTest {

    private final UUID anna = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID bob = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private final UUID carol = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private final UUID dave = UUID.fromString("00000000-0000-0000-0000-00000000000d");

    @Test
    void nothingToDoWhenEveryoneIsSettled() {
        assertThat(DebtSimplifier.simplify(Map.of(anna, 0L, bob, 0L))).isEmpty();
        assertThat(DebtSimplifier.simplify(Map.of())).isEmpty();
    }

    @Test
    void oneDebtorPaysOneCreditor() {
        assertThat(DebtSimplifier.simplify(Map.of(anna, 0L, bob, 3000L, carol, -3000L)))
                .containsExactly(new Transfer(carol, bob, 3000));
    }

    @Test
    void biggestDebtorPaysBiggestCreditorFirst() {
        // Anna is owed 50, Bob 10; Carol owes 40, Dave owes 20
        List<Transfer> transfers = DebtSimplifier.simplify(Map.of(anna, 5000L, bob, 1000L, carol, -4000L, dave, -2000L));

        assertThat(transfers).containsExactly(
                new Transfer(carol, anna, 4000),
                new Transfer(dave, anna, 1000),
                new Transfer(dave, bob, 1000));
    }

    @Test
    void neverNeedsMoreThanNMinusOneTransfersAndSettlesEveryone() {
        Random random = new Random(3);
        for (int run = 0; run < 500; run++) {
            Map<UUID, Long> balances = randomBalancesAddingUpToZero(random, random.nextInt(2, 12));

            List<Transfer> transfers = DebtSimplifier.simplify(balances);

            assertThat(transfers).hasSizeLessThanOrEqualTo(balances.size() - 1);
            assertThat(transfers).allSatisfy(t -> assertThat(t.amountMinor()).isPositive());
            // Applying the transfers brings everyone to zero
            Map<UUID, Long> after = new HashMap<>(balances);
            transfers.forEach(t -> {
                after.merge(t.fromUserId(), t.amountMinor(), Long::sum);
                after.merge(t.toUserId(), -t.amountMinor(), Long::sum);
            });
            assertThat(after.values()).allMatch(balance -> balance == 0);
        }
    }

    @Test
    void sameBalancesAlwaysGiveTheSamePlan() {
        Map<UUID, Long> balances = Map.of(anna, 1000L, bob, 1000L, carol, -1000L, dave, -1000L);

        assertThat(DebtSimplifier.simplify(balances)).isEqualTo(DebtSimplifier.simplify(new HashMap<>(balances)));
    }

    @Test
    void rejectsBalancesThatDoNotAddUpToZero() {
        assertThatThrownBy(() -> DebtSimplifier.simplify(Map.of(anna, 100L, bob, -50L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Map<UUID, Long> randomBalancesAddingUpToZero(Random random, int people) {
        Map<UUID, Long> balances = new HashMap<>();
        long sum = 0;
        for (int i = 0; i < people - 1; i++) {
            long balance = random.nextLong(-100_000, 100_000);
            balances.put(UUID.randomUUID(), balance);
            sum += balance;
        }
        balances.put(UUID.randomUUID(), -sum);
        return balances;
    }
}
