package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.settlement.SettlementRequest;
import io.github.eruden24.fairshare.dto.settlement.SettlementResponse;
import io.github.eruden24.fairshare.dto.settlement.SuggestedTransferResponse;
import io.github.eruden24.fairshare.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Settlements", description = "Payments between members to settle up")
@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "Record a payment between two members")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementResponse create(@CurrentUserId UUID currentUserId,
                                     @PathVariable UUID groupId,
                                     @Valid @RequestBody SettlementRequest request) {
        return settlementService.create(currentUserId, groupId, request);
    }

    @Operation(summary = "List settlements, newest first")
    @GetMapping
    public List<SettlementResponse> list(@CurrentUserId UUID currentUserId,
                                         @PathVariable UUID groupId) {
        return settlementService.list(currentUserId, groupId);
    }

    @Operation(summary = "Suggested transfers that would settle everyone up")
    @GetMapping("/suggested")
    public List<SuggestedTransferResponse> suggest(@CurrentUserId UUID currentUserId,
                                                   @PathVariable UUID groupId) {
        return settlementService.suggest(currentUserId, groupId);
    }

    @Operation(summary = "Delete a settlement (whoever recorded it, or the owner)")
    @DeleteMapping("/{settlementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId UUID currentUserId,
                       @PathVariable UUID groupId,
                       @PathVariable UUID settlementId) {
        settlementService.delete(currentUserId, groupId, settlementId);
    }
}
