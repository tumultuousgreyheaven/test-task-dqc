package com.example.dqc.domain.rule;

import com.example.dqc.domain.model.Finding;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.IssueKey;
import com.example.dqc.domain.model.QualitySnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Применяет набор правил к снимку и возвращает найденные проблемы.
 *
 * <p>Не знает ни про БД, ни про API, ни про конкретные правила: набор приходит снаружи.
 * Правила применяются в порядке кодов, поэтому результат детерминирован независимо от того,
 * в каком порядке их передали.
 */
public final class RuleEngine {

    private final List<QualityRule> rules;

    public RuleEngine(Collection<? extends QualityRule> rules) {
        List<QualityRule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparing(QualityRule::code));
        Set<String> codes = new HashSet<>();
        for (QualityRule rule : sorted) {
            if (!codes.add(rule.code())) {
                throw new IllegalArgumentException("Два правила с кодом " + rule.code());
            }
        }
        this.rules = List.copyOf(sorted);
    }

    public List<QualityRule> rules() {
        return rules;
    }

    /**
     * @throws IllegalStateException если правило вернуло две находки по одному объекту
     *                               (в {@code kkd_issue} такая пара нарушила бы уникальность)
     */
    public List<Issue> run(QualitySnapshot snapshot) {
        List<Issue> issues = new ArrayList<>();
        Set<IssueKey> seen = new HashSet<>();
        for (QualityRule rule : rules) {
            for (Finding finding : rule.check(snapshot)) {
                Issue issue = new Issue(
                        rule.code(),
                        rule.severity(),
                        finding.objectType(),
                        finding.objectId(),
                        finding.goalId(),
                        finding.unitId(),
                        finding.message());
                if (!seen.add(issue.key())) {
                    throw new IllegalStateException("Правило %s вернуло несколько находок по объекту %s %s"
                            .formatted(rule.code(), finding.objectType().code(), finding.objectId()));
                }
                issues.add(issue);
            }
        }
        return List.copyOf(issues);
    }
}
