package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.balance.GroupBalancesResponse;
import io.github.eruden24.fairshare.dto.balance.MyBalanceResponse;
import io.github.eruden24.fairshare.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping("/groups/{groupId}/balances")
    public GroupBalancesResponse forGroup(@CurrentUserId UUID currentUserId,
                                          @PathVariable UUID groupId) {
        return balanceService.forGroup(currentUserId, groupId);
    }

    @GetMapping("/users/me/balances")
    public List<MyBalanceResponse> mine(@CurrentUserId UUID currentUserId) {
        return balanceService.forUser(currentUserId);
    }
}
