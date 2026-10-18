package io.github.eruden24.fairshare.dto.expense;

import io.github.eruden24.fairshare.enumeration.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Used to create an expense and to replace one (PUT). The payer doesn't have to be a participant.
 * The amount is in the group's currency; how many decimals it may have depends on that currency,
 * so @Digits only sets an upper bound (3 decimals, e.g. KWD) and the service checks the rest.
 */
public record ExpenseRequest(
    @NotBlank
    @Size(max=200)
    String description,

    @NotNull
    @Positive
    @Digits(integer=12, fraction=3)
    BigDecimal amount,

    @NotNull
    UUID paidById,

    // Optional: defaults to EQUAL
    SplitType splitType,

    @NotEmpty
    List<@NotNull @Valid ParticipantRequest> participants,

    // Optional: defaults to today
    LocalDate date
) {}
