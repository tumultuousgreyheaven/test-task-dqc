package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.KeyResult;
import com.example.dqc.domain.model.ObjectType;
import com.example.dqc.domain.model.Severity;

import java.util.Objects;

/** Общая часть правил: код, важность и фабрики находок. */
public abstract class BaseQualityRule implements QualityRule {

    private final String code;
    private final Severity severity;

    protected BaseQualityRule(String code, Severity severity) {
        this.code = Objects.requireNonNull(code, "code");
        this.severity = Objects.requireNonNull(severity, "severity");
    }

    @Override
    public final String code() {
        return code;
    }

    @Override
    public final Severity severity() {
        return severity;
    }

    /** Находка по цели. */
    protected static Finding goalFinding(Goal goal, String message) {
        return new Finding(ObjectType.GOAL, goal.id(), goal.id(), goal.unitId(), message);
    }

    /** Находка по КР; подразделение и цель берутся у цели, которой принадлежит КР. */
    protected static Finding keyResultFinding(Goal goal, KeyResult keyResult, String message) {
        return new Finding(ObjectType.KEY_RESULT, keyResult.id(), goal.id(), goal.unitId(), message);
    }
}
