package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.balance.GroupBalancesResponse;
import io.github.eruden24.fairshare.dto.balance.MyBalanceResponse;
import io.github.eruden24.fairshare.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Balances", description = "Who is owed and who owes: paid - owed + sent - received")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @Operation(summary = "Everyone's balance in a group")
    @GetMapping("/groups/{groupId}/balances")
    public GroupBalancesResponse forGroup(@CurrentUserId UUID currentUserId,
                                          @PathVariable UUID groupId) {
        return balanceService.forGroup(currentUserId, groupId);
    }

    @Operation(summary = "My balance in each of my groups")
    @GetMapping("/users/me/balances")
    public List<MyBalanceResponse> mine(@CurrentUserId UUID currentUserId) {
        return balanceService.forUser(currentUserId);
    }
}
