package com.example.dqc.application.model;

import com.example.dqc.domain.model.OrgUnit;

import java.util.List;
import java.util.Objects;

/**
 * Сводка ККД по подразделению (с дочерними) за прогон.
 *
 * @param comparedTo null у первого прогона
 * @param delta      null у первого прогона: сравнивать не с чем, а "0 новых, 0 исправленных" было бы неправдой
 */
public record QualitySummary(
        RunRef run,
        RunRef comparedTo,
        OrgUnit unit,
        List<MetricSummary> metrics,
        List<SeveritySummary> issuesBySeverity,
        DeltaCounts delta) {

    public QualitySummary {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(unit, "unit");
        metrics = List.copyOf(metrics);
        issuesBySeverity = List.copyOf(issuesBySeverity);
    }
}
