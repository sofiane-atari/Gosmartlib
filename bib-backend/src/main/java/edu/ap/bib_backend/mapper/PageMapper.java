package edu.ap.bib_backend.mapper;

import org.springframework.data.domain.Page;

import edu.ap.bib_backend.dto.PaginatedMeta;

public class PageMapper {

    public static PaginatedMeta toPaginatedMeta(Page<?> page) {
        return new PaginatedMeta(
                page.getNumber(), // currentPage
                page.getSize(), // pageSize
                (int) page.getTotalElements(), // totalItems
                page.getTotalPages() // totalPages
        );
    }
}