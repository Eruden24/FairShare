package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.expense.CreateExpenseRequest;
import io.github.eruden24.fairshare.dto.expense.ExpenseResponse;
import io.github.eruden24.fairshare.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@CurrentUserId UUID currentUserId,
                                  @PathVariable UUID groupId,
                                  @Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.create(currentUserId, groupId, request);
    }

    @GetMapping("/{expenseId}")
    public ExpenseResponse get(@CurrentUserId UUID currentUserId,
                               @PathVariable UUID groupId,
                               @PathVariable UUID expenseId) {
        return expenseService.get(currentUserId, groupId, expenseId);
    }
}
