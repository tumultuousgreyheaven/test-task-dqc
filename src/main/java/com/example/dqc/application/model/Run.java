package com.example.dqc.application.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Прогон ККД.
 *
 * @param comparedToRunId прогон, с которым сравнивается этот; null у первого прогона
 */
public record Run(String id, LocalDate snapshotDate, RunStatus status, String comparedToRunId) {

    public Run {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(snapshotDate, "snapshotDate");
        Objects.requireNonNull(status, "status");
    }
}
