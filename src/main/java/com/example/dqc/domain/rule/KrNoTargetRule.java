package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.KeyResult;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.model.Severity;

import java.util.ArrayList;
import java.util.List;

/** KR_NO_TARGET (major): у КР не заполнено целевое значение. Значение 0 считается заполненным. */
public final class KrNoTargetRule extends BaseQualityRule {

    public static final String CODE = "KR_NO_TARGET";

    public KrNoTargetRule() {
        super(CODE, Severity.MAJOR);
    }

    @Override
    public List<Finding> check(QualitySnapshot snapshot) {
        List<Finding> findings = new ArrayList<>();
        for (Goal goal : snapshot.activeGoals()) {
            for (KeyResult keyResult : snapshot.keyResultsOf(goal.id())) {
                if (keyResult.targetValue() == null) {
                    findings.add(keyResultFinding(goal, keyResult,
                            "У КР %s цели %s «%s» не заполнено целевое значение"
                                    .formatted(keyResult.id(), goal.id(), keyResult.title())));
                }
            }
        }
        return findings;
    }
}
