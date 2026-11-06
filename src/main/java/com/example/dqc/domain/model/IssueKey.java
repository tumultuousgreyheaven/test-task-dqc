package com.example.dqc.domain.model;

import java.util.Objects;

/** Идентичность проблемы между прогонами: "та же самая" = то же правило и тот же объект. */
public record IssueKey(String ruleCode, ObjectType objectType, String objectId) {

    public IssueKey {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(objectType, "objectType");
        Objects.requireNonNull(objectId, "objectId");
    }
}
