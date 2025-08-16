package com.halcyon.recurix.service.pagination;

import java.util.List;

public record Page<T> (
                       List<T> content,
                       int currentPage,
                       int totalPages,
                       int totalElements) {}
