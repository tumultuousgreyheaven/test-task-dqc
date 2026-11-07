package com.example.dqc.application.model;

import java.time.LocalDate;
import java.util.Objects;

/** Ссылка на прогон: поля "run" и "compared_to" ответа сводки. */
public record RunRef(String id, LocalDate snapshotDate) {

    public RunRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(snapshotDate, "snapshotDate");
    }

    public static RunRef of(Run run) {
        return new RunRef(run.id(), run.snapshotDate());
    }
}
