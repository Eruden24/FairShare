package io.github.eruden24.fairshare.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Only the name can change. The currency is fixed once the group exists,
 * because every expense in the group is recorded in it.
 */
public record UpdateGroupRequest(
    @NotBlank
    @Size(max=100)
    String name
) {}
