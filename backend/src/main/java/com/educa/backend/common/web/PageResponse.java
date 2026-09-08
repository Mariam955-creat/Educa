package com.educa.backend.common.web;

import java.util.List;

import org.springframework.data.domain.Page;

/** Enveloppe de pagination renvoyée par l'API (voir docs/02-conception.md §4). */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
