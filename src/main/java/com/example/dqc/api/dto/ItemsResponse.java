package com.example.dqc.api.dto;

import java.util.List;

/** Список без постраничности: {@code {"items": [...]}}. */
public record ItemsResponse<T>(List<T> items) {
}
