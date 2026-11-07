package com.example.dqc.application.model;

import com.example.dqc.domain.model.Severity;

import java.util.Objects;

/**
 * Число проблем одной важности.
 *
 * @param previous число в прошлом прогоне; null, если сравнивать не с чем (первый прогон)
 * @param delta    current - previous; null, если previous null
 */
public record SeveritySummary(Severity severity, int current, Integer previous, Integer delta) {

    public SeveritySummary {
        Objects.requireNonNull(severity, "severity");
    }
}
