package com.example.dqc.domain.metric;

import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.QualitySnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Каскадная доля корректных целей: активные цели, у которых нет проблем ни у самой цели, ни у
 * любой вышестоящей цели по цепочке {@code parent_goal_id}, от всех активных целей.
 *
 * <p>Это учёт каскадирования: цель, выросшая из "грязной" цели, не может считаться надёжной.
 * Значение всегда не больше {@code clean_goals_share}, а разница — это "унаследованный" брак.
 * Неактивная цель в цепочке проблем иметь не может (правила её не проверяют), поэтому сама по себе
 * ничего не портит, но цепочка через неё продолжается. Цикл в данных обрывается.
 */
public final class CascadeCleanShareMetric implements QualityMetric {

    public static final String KEY = "cascade_clean_share";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String title() {
        return "Доля целей без проблем в цепочке вышестоящих";
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
        Set<String> goalsWithIssues = issues.stream().map(Issue::goalId).collect(Collectors.toSet());
        RatioAccumulator accumulator = new RatioAccumulator();
        for (Goal goal : snapshot.activeGoals()) {
            accumulator.add(goal.unitId(), isChainClean(goal, snapshot, goalsWithIssues));
        }
        return accumulator.result();
    }

    private static boolean isChainClean(Goal goal, QualitySnapshot snapshot, Set<String> goalsWithIssues) {
        Set<String> visited = new HashSet<>();
        Goal current = goal;
        while (current != null && visited.add(current.id())) {
            if (goalsWithIssues.contains(current.id())) {
                return false;
            }
            current = current.parentGoalId() == null ? null : snapshot.goal(current.parentGoalId()).orElse(null);
        }
        return true;
    }
}
