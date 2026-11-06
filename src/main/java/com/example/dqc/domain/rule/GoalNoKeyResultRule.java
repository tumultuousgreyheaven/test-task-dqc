package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.model.Severity;

import java.util.List;

/** GOAL_NO_KR (critical): у активной цели нет ни одного ключевого результата. */
public final class GoalNoKeyResultRule extends BaseQualityRule {

    public static final String CODE = "GOAL_NO_KR";

    public GoalNoKeyResultRule() {
        super(CODE, Severity.CRITICAL);
    }

    @Override
    public List<Finding> check(QualitySnapshot snapshot) {
        return snapshot.activeGoals().stream()
                .filter(goal -> snapshot.keyResultsOf(goal.id()).isEmpty())
                .map(goal -> goalFinding(goal,
                        "У цели %s «%s» нет ни одного ключевого результата".formatted(goal.id(), goal.title())))
                .toList();
    }
}
