package io.github.eruden24.fairshare.entity;

import io.github.eruden24.fairshare.enumeration.GroupRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private GroupRole role;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public GroupMember(ExpenseGroup group, User user, GroupRole role) {
        this.group = group;
        this.user = user;
        this.role = role;
    }
}