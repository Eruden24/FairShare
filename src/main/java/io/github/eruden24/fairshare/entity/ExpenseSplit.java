package io.github.eruden24.fairshare.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * One participant's share of an expense, in minor units. Stored rather than recomputed,
 * because who gets the leftover cent is chosen randomly.
 */
@Entity
@Table(name = "expense_splits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false, updatable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    ExpenseSplit(Expense expense, User user, long amountMinor) {
        this.expense = expense;
        this.user = user;
        this.amountMinor = amountMinor;
    }
}
