package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.OrgUnit;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class RuleEngineTest {

    @Test
    void runsRulesInCodeOrderAndAddsRuleMetadataToFindings() {
        QualityRule zRule = rule("Z_RULE", Severity.MINOR, "G-1");
        QualityRule aRule = rule("A_RULE", Severity.CRITICAL, "G-1");
        QualitySnapshot snapshot = new QualitySnapshot(LocalDate.of(2026, 11, 7),
                List.of(new OrgUnit("U", "Unit", null)), List.of(), List.of());

        var issues = new RuleEngine(List.of(zRule, aRule)).run(snapshot);

        assertThat(issues).extracting(issue -> issue.ruleCode()).containsExactly("A_RULE", "Z_RULE");
        assertThat(issues).extracting(issue -> issue.severity())
                .containsExactly(Severity.CRITICAL, Severity.MINOR);
    }

    @Test
    void rejectsDuplicateRuleCodesOnConstruction() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new RuleEngine(List.of(rule("DUPLICATE", Severity.MAJOR, "G-1"),
                        rule("DUPLICATE", Severity.MINOR, "G-2"))))
                .withMessageContaining("Два правила с кодом DUPLICATE");
    }

    @Test
    void rejectsTwoFindingsWithSameRuleAndObject() {
        QualityRule duplicateFindingRule = new QualityRule() {
            @Override
            public String code() {
                return "DUPLICATE_FINDING";
            }

            @Override
            public Severity severity() {
                return Severity.MAJOR;
            }

            @Override
            public List<Finding> check(QualitySnapshot snapshot) {
                return List.of(
                        new Finding(com.example.dqc.domain.model.ObjectType.GOAL, "G-1", "G-1", "U", "first"),
                        new Finding(com.example.dqc.domain.model.ObjectType.GOAL, "G-1", "G-1", "U", "second"));
            }
        };
        QualitySnapshot snapshot = new QualitySnapshot(LocalDate.of(2026, 11, 7),
                List.of(new OrgUnit("U", "Unit", null)), List.of(), List.of());

        assertThatIllegalStateException().isThrownBy(() -> new RuleEngine(List.of(duplicateFindingRule)).run(snapshot))
                .withMessageContaining("вернуло несколько находок");
    }

    private static QualityRule rule(String code, Severity severity, String goalId) {
        return new QualityRule() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public Severity severity() {
                return severity;
            }

            @Override
            public List<Finding> check(QualitySnapshot snapshot) {
                return List.of(new Finding(com.example.dqc.domain.model.ObjectType.GOAL,
                        goalId, goalId, "U", code));
            }
        };
    }
}
