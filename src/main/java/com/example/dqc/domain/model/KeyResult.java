package com.example.dqc.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Ключевой результат. Вес — в процентах (0..100). {@code targetValue == null} означает
 * «целевое значение не заполнено»; значение 0 при этом считается заполненным.
 */
public record KeyResult(
        String id,
        String goalId,
        String title,
        BigDecimal targetValue,
        BigDecimal actualValue,
        BigDecimal weight,
        LocalDate factUpdatedAt) {

    public KeyResult {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(goalId, "goalId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(weight, "weight");
    }
}
