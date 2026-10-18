package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.PageResponse;
import io.github.eruden24.fairshare.dto.expense.ExpenseRequest;
import io.github.eruden24.fairshare.dto.expense.ExpenseResponse;
import io.github.eruden24.fairshare.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Expenses", description = "Expenses of a group and how they are split")
@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(summary = "Add an expense (split EQUAL, EXACT, PERCENTAGE or SHARES)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@CurrentUserId UUID currentUserId,
                                  @PathVariable UUID groupId,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(currentUserId, groupId, request);
    }

    /** Newest first. from/to are inclusive dates (2026-10-01), paidById filters by payer. */
    @Operation(summary = "List expenses, newest first, with optional date and payer filters")
    @GetMapping
    public PageResponse<ExpenseResponse> list(@CurrentUserId UUID currentUserId,
                                              @PathVariable UUID groupId,
                                              @RequestParam(required = false) LocalDate from,
                                              @RequestParam(required = false) LocalDate to,
                                              @RequestParam(required = false) UUID paidById,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return expenseService.list(currentUserId, groupId, from, to, paidById, page, size);
    }

    @Operation(summary = "Expense details with each participant's share")
    @GetMapping("/{expenseId}")
    public ExpenseResponse get(@CurrentUserId UUID currentUserId,
                               @PathVariable UUID groupId,
                               @PathVariable UUID expenseId) {
        return expenseService.get(currentUserId, groupId, expenseId);
    }

    @Operation(summary = "Replace an expense (creator or owner; send version to avoid overwriting someone else's edit)")
    @PutMapping("/{expenseId}")
    public ExpenseResponse update(@CurrentUserId UUID currentUserId,
                                  @PathVariable UUID groupId,
                                  @PathVariable UUID expenseId,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(currentUserId, groupId, expenseId, request);
    }

    @Operation(summary = "Delete an expense (creator or owner)")
    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId UUID currentUserId,
                       @PathVariable UUID groupId,
                       @PathVariable UUID expenseId) {
        expenseService.delete(currentUserId, groupId, expenseId);
    }
}
