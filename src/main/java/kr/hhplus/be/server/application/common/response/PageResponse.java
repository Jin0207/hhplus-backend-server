package kr.hhplus.be.server.application.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> contents,
        int currentPage,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
