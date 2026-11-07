package com.example.dqc.api.dto;

import com.example.dqc.application.model.DeltaCounts;
import com.example.dqc.application.model.MetricSummary;
import com.example.dqc.application.model.QualitySummary;
import com.example.dqc.application.model.RunRef;
import com.example.dqc.application.model.SeveritySummary;
import com.example.dqc.domain.metric.MetricReading;
import com.example.dqc.domain.model.OrgUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ответ GET /api/kkd/summary, поле в поле по summary-response-format.json.
 * Поля, у которых нет значения (первый прогон, метрика без объектов), отдаются как null, а не опускаются.
 */
public record SummaryResponse(
        RunRefDto run,
        RunRefDto comparedTo,
        UnitDto unit,
        List<MetricDto> metrics,
        List<SeverityDto> issuesBySeverity,
        DeltaDto delta) {

    public record RunRefDto(String id, LocalDate snapshotDate) {
        static RunRefDto from(RunRef ref) {
            return ref == null ? null : new RunRefDto(ref.id(), ref.snapshotDate());
        }
    }

    public record UnitDto(String id, String name) {
        static UnitDto from(OrgUnit unit) {
            return new UnitDto(unit.id(), unit.name());
        }
    }

    public record MetricDto(String key, String title, String unit,
                            BigDecimal value, BigDecimal previous, BigDecimal delta, String trend) {
        static MetricDto from(MetricSummary metric) {
            MetricReading reading = metric.reading();
            return new MetricDto(metric.key(), metric.title(), metric.unit(),
                    reading.value(), reading.previous(), reading.delta(),
                    reading.trend() == null ? null : reading.trend().code());
        }
    }

    public record SeverityDto(String code, int current, Integer previous, Integer delta) {
        static SeverityDto from(SeveritySummary severity) {
            return new SeverityDto(severity.severity().code(), severity.current(), severity.previous(), severity.delta());
        }
    }

    /** Поле {@code new} — ключевое слово Java, поэтому в коде оно называется иначе. */
    @JsonPropertyOrder({"new", "persisting", "resolved"})
    public record DeltaDto(@JsonProperty("new") int newIssues, int persisting, int resolved) {
        static DeltaDto from(DeltaCounts counts) {
            return counts == null ? null : new DeltaDto(counts.newIssues(), counts.persisting(), counts.resolved());
        }
    }

    public static SummaryResponse from(QualitySummary summary) {
        return new SummaryResponse(
                RunRefDto.from(summary.run()),
                RunRefDto.from(summary.comparedTo()),
                UnitDto.from(summary.unit()),
                summary.metrics().stream().map(MetricDto::from).toList(),
                summary.issuesBySeverity().stream().map(SeverityDto::from).toList(),
                DeltaDto.from(summary.delta()));
    }
}
