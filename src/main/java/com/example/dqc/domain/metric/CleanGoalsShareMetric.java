package com.example.dqc.domain.metric;

import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.QualitySnapshot;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Доля корректных целей: активные цели, по которым нет ни одной проблемы
 * (ни по самой цели, ни по её КР), от всех активных целей. Учитывает все правила.
 */
public final class CleanGoalsShareMetric implements QualityMetric {

    public static final String KEY = "clean_goals_share";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String title() {
        return "Доля корректных целей";
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
        // goalId есть и у проблем КР, поэтому проблема КР делает "некорректной" всю цель
        Set<String> goalsWithIssues = issues.stream().map(Issue::goalId).collect(Collectors.toSet());
        RatioAccumulator accumulator = new RatioAccumulator();
        for (Goal goal : snapshot.activeGoals()) {
            accumulator.add(goal.unitId(), !goalsWithIssues.contains(goal.id()));
        }
        return accumulator.result();
    }
}
