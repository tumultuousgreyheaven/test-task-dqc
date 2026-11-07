package com.example.dqc.domain.delta;

import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.ObjectType;
import com.example.dqc.domain.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IssueDeltaCalculatorTest {

    @Test
    void matchesIssuesOnlyByRuleAndObjectAndSeparatesNewPersistingAndResolved() {
        Issue persistsBefore = issue("GOAL_NO_OWNER", Severity.CRITICAL,
                ObjectType.GOAL, "G-1", "G-1", "UNIT-OLD", "old message");
        Issue resolved = issue("GOAL_NO_KR", Severity.CRITICAL,
                ObjectType.GOAL, "G-2", "G-2", "UNIT-1", "resolved");

        // Severity, unit and message changed, but the rule and the object are still the same.
        Issue persistsNow = issue("GOAL_NO_OWNER", Severity.MAJOR,
                ObjectType.GOAL, "G-1", "G-1", "UNIT-NEW", "updated message");
        Issue newIssue = issue("KR_NO_TARGET", Severity.MAJOR,
                ObjectType.KEY_RESULT, "KR-3", "G-3", "UNIT-1", "new");

        IssueDelta delta = IssueDeltaCalculator.calculate(
                List.of(persistsNow, newIssue), List.of(persistsBefore, resolved));

        assertThat(delta.newIssues()).containsExactly(newIssue);
        assertThat(delta.persisting()).containsExactly(persistsNow);
        assertThat(delta.resolved()).containsExactly(resolved);
    }

    @Test
    void objectTypeIsPartOfIssueIdentity() {
        Issue current = issue("SAME_RULE", Severity.MAJOR,
                ObjectType.KEY_RESULT, "123", "G-1", "U", "current");
        Issue previous = issue("SAME_RULE", Severity.MAJOR,
                ObjectType.GOAL, "123", "123", "U", "previous");

        IssueDelta delta = IssueDeltaCalculator.calculate(List.of(current), List.of(previous));

        assertThat(delta.newIssues()).containsExactly(current);
        assertThat(delta.persisting()).isEmpty();
        assertThat(delta.resolved()).containsExactly(previous);
    }

    @Test
    void emptyPreviousCollectionMakesAllCurrentIssuesNew() {
        Issue current = issue("RULE", Severity.MINOR, ObjectType.GOAL, "G-1", "G-1", "U", "message");

        IssueDelta delta = IssueDeltaCalculator.calculate(List.of(current), List.of());

        assertThat(delta.newIssues()).containsExactly(current);
        assertThat(delta.persisting()).isEmpty();
        assertThat(delta.resolved()).isEmpty();
    }

    private static Issue issue(String rule, Severity severity, ObjectType type,
                               String objectId, String goalId, String unitId, String message) {
        return new Issue(rule, severity, type, objectId, goalId, unitId, message);
    }
}
