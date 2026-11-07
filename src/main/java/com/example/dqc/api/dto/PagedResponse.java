package com.example.dqc.api.dto;

import java.util.List;

/** Страница списка: {@code {"items": [...], "total": N, "limit": N, "offset": N}}. */
public record PagedResponse<T>(List<T> items, long total, int limit, int offset) {
}
