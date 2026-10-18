package io.github.eruden24.fairshare.entity;

import io.github.eruden24.fairshare.enumeration.SplitType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An amount paid by one member and shared between participants. Amounts are minor units
 * in the group's currency; the splits always add up to amountMinor.
 */
@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @Setter
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Setter
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @Setter
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseSplit> splits = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Expense(Group group, String description, long amountMinor, User paidBy, LocalDate expenseDate,
                   SplitType splitType, User createdBy) {
        this.group = group;
        this.description = description;
        this.amountMinor = amountMinor;
        this.paidBy = paidBy;
        this.expenseDate = expenseDate;
        this.splitType = splitType;
        this.createdBy = createdBy;
    }

    /** One participant's share, as computed by the split. value is what they entered (PERCENTAGE, SHARES). */
    public record Share(User user, long amountMinor, BigDecimal value) {}

    /**
     * Sets the participants and their shares. Rows of people who stay are updated in place instead of
     * deleted and re-inserted: Hibernate runs inserts before deletes when flushing, so re-inserting the
     * same person would break the one-split-per-person unique constraint.
     */
    public void replaceSplits(List<Share> shares) {
        Map<UUID, Share> byUser = new LinkedHashMap<>();
        shares.forEach(share -> byUser.put(share.user().getId(), share));

        splits.removeIf(split -> !byUser.containsKey(split.getUser().getId()));
        for (ExpenseSplit split : splits) {
            Share share = byUser.remove(split.getUser().getId());
            split.update(share.amountMinor(), share.value());
        }
        byUser.values().forEach(share ->
                splits.add(new ExpenseSplit(this, share.user(), share.amountMinor(), share.value())));
    }
}
