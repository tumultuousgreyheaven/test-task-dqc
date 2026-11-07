package com.example.dqc.domain.metric;

import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.GoalStatus;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.KeyResult;
import com.example.dqc.domain.model.ObjectType;
import com.example.dqc.domain.model.OrgUnit;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.model.Severity;
import com.example.dqc.domain.rule.GoalNoOwnerRule;
import com.example.dqc.domain.rule.KrNoTargetRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricCalculatorTest {

    @Test
    void calculatesCleanSharesWithCascadeThroughInactiveParent() {
        QualitySnapshot snapshot = new QualitySnapshot(LocalDate.of(2026, 11, 7),
                List.of(
                        new OrgUnit("HQ", "Head office", null),
                        new OrgUnit("TEAM", "Team", "HQ")),
                List.of(
                        goal("PARENT", "HQ", null, GoalStatus.ACTIVE),
                        goal("INACTIVE-BRIDGE", "HQ", "PARENT", GoalStatus.DRAFT),
                        goal("CHILD", "TEAM", "INACTIVE-BRIDGE", GoalStatus.ACTIVE),
                        goal("BROKEN-KR", "TEAM", null, GoalStatus.ACTIVE),
                        goal("CLEAN", "TEAM", null, GoalStatus.ACTIVE)),
                List.of(
                        keyResult("KR-PARENT", "PARENT", "10"),
                        keyResult("KR-CHILD", "CHILD", "10"),
                        keyResult("KR-BROKEN", "BROKEN-KR", null),
                        keyResult("KR-CLEAN", "CLEAN", "10")));

        List<Issue> issues = List.of(
                issue(GoalNoOwnerRule.CODE, Severity.CRITICAL, ObjectType.GOAL,
                        "PARENT", "PARENT", "HQ"),
                issue(KrNoTargetRule.CODE, Severity.MAJOR, ObjectType.KEY_RESULT,
                        "KR-BROKEN", "BROKEN-KR", "TEAM"));
        MetricCalculator calculator = new MetricCalculator(List.of(
                new CascadeCleanShareMetric(), new CleanGoalsShareMetric(), new KrWithTargetShareMetric()));

        List<MetricValue> values = calculator.calculate(snapshot, issues);

        assertThat(value(values, CascadeCleanShareMetric.KEY, "HQ")).isEqualTo(new Ratio(0, 1));
        // CHILD inherits the issue from PARENT only in the cascade metric. The ordinary clean-goals
        // metric checks issues attached to the goal itself (or its key results), so CHILD is clean there.
        assertThat(value(values, CascadeCleanShareMetric.KEY, "TEAM")).isEqualTo(new Ratio(1, 3));
        assertThat(value(values, CleanGoalsShareMetric.KEY, "TEAM")).isEqualTo(new Ratio(2, 3));
        assertThat(value(values, KrWithTargetShareMetric.KEY, "TEAM")).isEqualTo(new Ratio(2, 3));
        assertThat(value(values, KrWithTargetShareMetric.KEY, "HQ")).isEqualTo(new Ratio(1, 1));
    }

    @Test
    void storesZeroOverZeroForUnitsWithoutApplicableObjects() {
        QualitySnapshot snapshot = new QualitySnapshot(LocalDate.of(2026, 11, 7),
                List.of(new OrgUnit("EMPTY", "Empty", null)), List.of(), List.of());

        List<MetricValue> values = new MetricCalculator(List.of(new CleanGoalsShareMetric()))
                .calculate(snapshot, List.of());

        assertThat(values).containsExactly(new MetricValue(CleanGoalsShareMetric.KEY, "EMPTY", Ratio.ZERO));
        assertThat(value(values, CleanGoalsShareMetric.KEY, "EMPTY").percent()).isEmpty();
    }

    private static Goal goal(String id, String unitId, String parentGoalId, GoalStatus status) {
        return new Goal(id, unitId, id, "Owner", parentGoalId, status, null);
    }

    private static KeyResult keyResult(String id, String goalId, String targetValue) {
        return new KeyResult(id, goalId, id,
                targetValue == null ? null : new BigDecimal(targetValue), null, BigDecimal.valueOf(100), null);
    }

    private static Issue issue(String rule, Severity severity, ObjectType type,
                               String objectId, String goalId, String unitId) {
        return new Issue(rule, severity, type, objectId, goalId, unitId, "test issue");
    }

    private static Ratio value(List<MetricValue> values, String metricKey, String unitId) {
        return values.stream()
                .filter(value -> value.metricKey().equals(metricKey) && value.unitId().equals(unitId))
                .map(MetricValue::ratio)
                .findFirst()
                .orElseThrow();
    }
}
