package com.example.dqc.domain.metric;

import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.KeyResult;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.rule.KrNoTargetRule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Доля КР с заполненным целевым значением: КР активных целей без находки KR_NO_TARGET
 * от всех КР активных целей. Опирается на правило KR_NO_TARGET.
 */
public final class KrWithTargetShareMetric implements QualityMetric {

    public static final String KEY = "kr_with_target_share";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String title() {
        return "Доля КР с заполненным целевым значением";
    }

    @Override
    public String unit() {
        return "%";
    }

    @Override
    public boolean higherIsBetter() {
        return true;
    }

    @Override
    public Map<String, Ratio> measure(QualitySnapshot snapshot, List<Issue> issues) {
        Set<String> keyResultsWithoutTarget = issues.stream()
                .filter(issue -> issue.ruleCode().equals(KrNoTargetRule.CODE))
                .map(Issue::objectId)
                .collect(Collectors.toSet());
        RatioAccumulator accumulator = new RatioAccumulator();
        for (Goal goal : snapshot.activeGoals()) {
            for (KeyResult keyResult : snapshot.keyResultsOf(goal.id())) {
                accumulator.add(goal.unitId(), !keyResultsWithoutTarget.contains(keyResult.id()));
            }
        }
        return accumulator.result();
    }
}
