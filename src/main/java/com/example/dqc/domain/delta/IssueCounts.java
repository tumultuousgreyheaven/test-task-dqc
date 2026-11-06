package com.example.dqc.domain.delta;

import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.Severity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** Подсчёт проблем для разбивки по важности. */
public final class IssueCounts {

    private IssueCounts() {
    }

    /** Число проблем по каждой важности; уровни без проблем присутствуют с нулём. */
    public static Map<Severity, Integer> bySeverity(Collection<Issue> issues) {
        Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
        for (Severity severity : Severity.values()) {
            counts.put(severity, 0);
        }
        for (Issue issue : issues) {
            counts.merge(issue.severity(), 1, Integer::sum);
        }
        return counts;
    }
}
