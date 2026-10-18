package io.github.eruden24.fairshare.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A page of results. Our own type instead of Spring's Page, whose JSON shape isn't guaranteed to stay stable.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
