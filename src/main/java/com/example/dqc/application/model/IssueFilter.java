package com.example.dqc.application.model;

import com.example.dqc.domain.model.Severity;

import java.util.Set;

/**
 * Условия отбора проблем прогона. Каждое условие необязательно (null = не ограничивать).
 *
 * @param unitIds подразделения, по которым отбирать (обычно поддерево выбранного подразделения);
 *                null — любые, пустое множество — не подходит ничего
 */
public record IssueFilter(Set<String> unitIds, String ruleCode, Severity severity) {

    public IssueFilter {
        unitIds = unitIds == null ? null : Set.copyOf(unitIds);
    }

    public static IssueFilter all() {
        return new IssueFilter(null, null, null);
    }

    /** true, если фильтр заведомо ничего не пропускает (пустое множество подразделений). */
    public boolean matchesNothing() {
        return unitIds != null && unitIds.isEmpty();
    }
}
