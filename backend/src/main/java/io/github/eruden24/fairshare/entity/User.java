package io.github.eruden24.fairshare.entity;


import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @Setter
    @Column(name="email", nullable = false)
    private String email;

    @Setter
    @Column(name="password_hash", nullable = false)
    private String passwordHash;

    @Setter
    @Column(name="display_name",  nullable = false)
    private String displayName;

    @Setter
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="default_currency",  nullable = false, length = 3)
    private String defaultCurrency = "EUR";

    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name="updated_at",  nullable = false)
    private Instant updatedAt;

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }
}
