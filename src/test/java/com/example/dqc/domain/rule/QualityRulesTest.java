package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.GoalStatus;
import com.example.dqc.domain.model.KeyResult;
import com.example.dqc.domain.model.ObjectType;
import com.example.dqc.domain.model.OrgUnit;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.model.Severity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QualityRulesTest {

    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 11, 7);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 12, 31);

    @Test
    void goalNoOwnerFindsNullEmptyAndWhitespaceOwnersButIgnoresInactiveGoals() {
        QualitySnapshot snapshot = snapshot(
                goal("G-NULL", null, GoalStatus.ACTIVE),
                goal("G-EMPTY", "", GoalStatus.ACTIVE),
                goal("G-BLANK", " \t ", GoalStatus.ACTIVE),
                goal("G-OWNED", "Alice", GoalStatus.ACTIVE),
                goal("G-INACTIVE", null, GoalStatus.DRAFT));

        List<Finding> findings = new GoalNoOwnerRule().check(snapshot);

        assertThat(findings).extracting(Finding::objectId)
                .containsExactly("G-NULL", "G-EMPTY", "G-BLANK");
        assertThat(findings).allSatisfy(finding -> {
            assertThat(finding.objectType()).isEqualTo(ObjectType.GOAL);
            assertThat(finding.goalId()).isEqualTo(finding.objectId());
            assertThat(finding.unitId()).isEqualTo("UNIT-1");
        });
        assertThat(new GoalNoOwnerRule().severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void goalNoKeyResultFindsOnlyActiveGoalsWithoutAnyKeyResults() {
        QualitySnapshot snapshot = snapshot(
                goal("G-WITHOUT-KR", "Alice", GoalStatus.ACTIVE),
                goal("G-WITH-KR", "Bob", GoalStatus.ACTIVE),
                goal("G-INACTIVE", "", GoalStatus.CANCELLED),
                keyResult("KR-1", "G-WITH-KR", bd("1"), bd("100")));

        List<Finding> findings = new GoalNoKeyResultRule().check(snapshot);

        assertThat(findings).extracting(Finding::objectId).containsExactly("G-WITHOUT-KR");
        assertThat(new GoalNoKeyResultRule().severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void krWeightsSumUsesExactDecimalComparisonAndDoesNotDuplicateMissingKrFinding() {
        QualitySnapshot snapshot = snapshot(
                goal("G-9999", "Alice", GoalStatus.ACTIVE),
                goal("G-100", "Bob", GoalStatus.ACTIVE),
                goal("G-NO-KR", "Carol", GoalStatus.ACTIVE),
                goal("G-INACTIVE", "", GoalStatus.ACHIEVED),
                keyResult("KR-1", "G-9999", bd("10"), bd("33.33")),
                keyResult("KR-2", "G-9999", bd("10"), bd("33.33")),
                keyResult("KR-3", "G-9999", bd("10"), bd("33.33")),
                keyResult("KR-4", "G-100", bd("40.0"), bd("40")),
                keyResult("KR-5", "G-100", bd("60.00"), bd("60")),
                keyResult("KR-6", "G-INACTIVE", bd("1"), bd("1")));

        List<Finding> findings = new KrWeightsSumRule().check(snapshot);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).objectId()).isEqualTo("G-9999");
        assertThat(findings.get(0).message()).contains("99.99%");
        assertThat(new KrWeightsSumRule().severity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    void krNoTargetTreatsZeroAsPresentAndChecksOnlyKeyResultsOfActiveGoals() {
        QualitySnapshot snapshot = snapshot(
                goal("G-ACTIVE", "Alice", GoalStatus.ACTIVE),
                goal("G-INACTIVE", "Bob", GoalStatus.DRAFT),
                keyResult("KR-MISSING", "G-ACTIVE", null, bd("50")),
                keyResult("KR-ZERO", "G-ACTIVE", bd("0"), bd("50")),
                keyResult("KR-INACTIVE", "G-INACTIVE", null, bd("100")));

        List<Finding> findings = new KrNoTargetRule().check(snapshot);

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.objectType()).isEqualTo(ObjectType.KEY_RESULT);
            assertThat(finding.objectId()).isEqualTo("KR-MISSING");
            assertThat(finding.goalId()).isEqualTo("G-ACTIVE");
            assertThat(finding.unitId()).isEqualTo("UNIT-1");
        });
        assertThat(new KrNoTargetRule().severity()).isEqualTo(Severity.MAJOR);
    }

    private static QualitySnapshot snapshot(Object... entities) {
        List<Goal> goals = new java.util.ArrayList<>();
        List<KeyResult> keyResults = new java.util.ArrayList<>();
        for (Object entity : entities) {
            if (entity instanceof Goal goal) {
                goals.add(goal);
            } else if (entity instanceof KeyResult keyResult) {
                keyResults.add(keyResult);
            } else {
                throw new IllegalArgumentException("Unsupported test entity: " + entity);
            }
        }
        return new QualitySnapshot(SNAPSHOT_DATE,
                List.of(new OrgUnit("UNIT-1", "Unit 1", null)), goals, keyResults);
    }

    private static Goal goal(String id, String owner, GoalStatus status) {
        return new Goal(id, "UNIT-1", id, owner, null, status, DUE_DATE);
    }

    private static KeyResult keyResult(String id, String goalId, BigDecimal target, BigDecimal weight) {
        return new KeyResult(id, goalId, id, target, null, weight, null);
    }

    private static BigDecimal bd(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
