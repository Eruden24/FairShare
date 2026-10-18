package io.github.eruden24.fairshare.settlement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Suggests who should pay whom so that every balance ends at zero.
 * <p>
 * Greedy: the person who owes the most pays the person who is owed the most, as much as one of them
 * needs, and repeat. Each step settles at least one person, so n people need at most n - 1 transfers.
 * That's not always the theoretical minimum (finding it is NP-hard) but it's what apps like Splitwise do,
 * and it's never worse than everyone paying everyone.
 */
public final class DebtSimplifier {

    /** amountMinor is always positive. */
    public record Transfer(UUID fromUserId, UUID toUserId, long amountMinor) {}

    private DebtSimplifier() {
    }

    /**
     * @param balances net balance per user in minor units: positive = is owed, negative = owes.
     *                 They must add up to zero.
     */
    public static List<Transfer> simplify(Map<UUID, Long> balances) {
        long sum = balances.values().stream().mapToLong(Long::longValue).sum();
        if (sum != 0) {
            throw new IllegalArgumentException("balances must add up to zero, got " + sum);
        }

        // Biggest amounts first; the user id breaks ties so the same balances always give the same plan
        Comparator<Position> biggestFirst = Comparator.comparingLong(Position::remaining).reversed()
                .thenComparing(Position::userId);
        List<Position> creditors = new ArrayList<>();
        List<Position> debtors = new ArrayList<>();
        balances.forEach((userId, balance) -> {
            if (balance > 0) {
                creditors.add(new Position(userId, balance));
            } else if (balance < 0) {
                debtors.add(new Position(userId, -balance));
            }
        });
        creditors.sort(biggestFirst);
        debtors.sort(biggestFirst);

        List<Transfer> transfers = new ArrayList<>();
        int c = 0;
        int d = 0;
        while (c < creditors.size() && d < debtors.size()) {
            Position creditor = creditors.get(c);
            Position debtor = debtors.get(d);
            long amount = Math.min(creditor.remaining, debtor.remaining);

            transfers.add(new Transfer(debtor.userId, creditor.userId, amount));
            creditor.remaining -= amount;
            debtor.remaining -= amount;

            if (creditor.remaining == 0) {
                c++;
            }
            if (debtor.remaining == 0) {
                d++;
            }
        }
        return transfers;
    }

    // How much someone is still owed, or still owes, while the plan is being built
    private static final class Position {
        private final UUID userId;
        private long remaining;

        Position(UUID userId, long remaining) {
            this.userId = userId;
            this.remaining = remaining;
        }

        UUID userId() {
            return userId;
        }

        long remaining() {
            return remaining;
        }
    }
}
