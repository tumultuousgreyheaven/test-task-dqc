package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.model.Severity;

import java.util.List;

/** GOAL_NO_OWNER (critical): у цели не назначен ответственный. Пустая строка и пробелы = не назначен. */
public final class GoalNoOwnerRule extends BaseQualityRule {

    public static final String CODE = "GOAL_NO_OWNER";

    public GoalNoOwnerRule() {
        super(CODE, Severity.CRITICAL);
    }

    @Override
    public List<Finding> check(QualitySnapshot snapshot) {
        return snapshot.activeGoals().stream()
                .filter(goal -> goal.owner() == null || goal.owner().isBlank())
                .map(goal -> goalFinding(goal,
                        "У цели %s «%s» не назначен ответственный".formatted(goal.id(), goal.title())))
                .toList();
    }
}
