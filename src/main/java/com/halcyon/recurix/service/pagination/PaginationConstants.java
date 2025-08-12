package com.halcyon.recurix.service.pagination;

import org.springframework.data.domain.Sort;

public final class PaginationConstants {

    private PaginationConstants() {}

    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final Sort DEFAULT_SORT = Sort.by("paymentDate").ascending();
}
