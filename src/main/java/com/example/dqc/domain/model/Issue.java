package com.example.dqc.domain.model;

import java.util.Objects;

/** Проблема, найденная правилом в конкретном прогоне. */
public record Issue(
        String ruleCode,
        Severity severity,
        ObjectType objectType,
        String objectId,
        String goalId,
        String unitId,
        String message) {

    public Issue {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(objectType, "objectType");
        Objects.requireNonNull(objectId, "objectId");
        Objects.requireNonNull(goalId, "goalId");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(message, "message");
    }

    public IssueKey key() {
        return new IssueKey(ruleCode, objectType, objectId);
    }
}
