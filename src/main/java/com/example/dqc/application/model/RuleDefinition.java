package com.example.dqc.application.model;

import com.example.dqc.domain.model.Severity;

import java.util.Objects;

/** Строка справочника правил (таблица kkd_rule). */
public record RuleDefinition(String code, String title, Severity severity, boolean active) {

    public RuleDefinition {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(severity, "severity");
    }
}
