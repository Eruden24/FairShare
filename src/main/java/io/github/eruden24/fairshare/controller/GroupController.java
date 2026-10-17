package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.dto.group.CreateGroupRequest;
import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.repository.GroupRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class ExpenseGroupController {

    private final GroupRepository expenseGroupRepository;

    @PostMapping
    public GroupResponse create(@Valid @RequestBody CreateGroupRequest request) {
        return expenseGroupRepository.create(request);
    }
}
