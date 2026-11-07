package com.example.dqc.application.model;

import java.util.List;

/**
 * Страница результатов.
 *
 * @param total сколько всего записей подходит под условия (без учёта limit и offset)
 */
public record PageResult<T>(List<T> items, long total) {

    public PageResult {
        items = List.copyOf(items);
    }
}
