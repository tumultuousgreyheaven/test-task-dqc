package com.example.dqc.domain.metric;

import java.math.BigDecimal;

/**
 * Значение метрики с прошлым значением и дельтой, как в ответе API. Любое поле может быть null:
 * {@code value} — нет объектов, по которым считать; {@code previous} — нет прошлого значения;
 * {@code delta} и {@code trend} — нечего сравнивать.
 */
public record MetricReading(BigDecimal value, BigDecimal previous, BigDecimal delta, Trend trend) {
}
