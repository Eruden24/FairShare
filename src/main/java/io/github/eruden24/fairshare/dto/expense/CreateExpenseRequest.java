package io.github.eruden24.fairshare.dto.expense;

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
 * Split equally between the participants. The payer doesn't have to be one of them.
 * The amount is in the group's currency; how many decimals it may have depends on that currency,
 * so @Digits only sets an upper bound (3 decimals, e.g. KWD) and the service checks the rest.
 */
public record CreateExpenseRequest(
    @NotBlank
    @Size(max=200)
    String description,

    @NotNull
    @Positive
    @Digits(integer=12, fraction=3)
    BigDecimal amount,

    @NotNull
    UUID paidById,

    @NotEmpty
    List<@NotNull UUID> participantIds,

    // Optional: defaults to today
    LocalDate date
) {}
