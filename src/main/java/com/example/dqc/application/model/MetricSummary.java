package com.example.dqc.application.model;

import com.example.dqc.domain.metric.MetricReading;

import java.util.Objects;

/** Метрика в сводке: описание и значение с прошлым значением и дельтой. */
public record MetricSummary(String key, String title, String unit, MetricReading reading) {

    public MetricSummary {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(reading, "reading");
    }
}
