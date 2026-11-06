package com.example.dqc.domain.delta;

import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.IssueKey;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сравнивает проблемы двух прогонов. Проблема "та же самая", если совпадают правило и объект
 * ({@link IssueKey}); текст сообщения, важность и подразделение в сравнении не участвуют.
 *
 * <p>Чтобы получить дельту по подразделению, оба набора сначала отбирают по его поддереву.
 * Если цель между прогонами перешла в другое подразделение, у нового это "новая" проблема,
 * у старого — "исправленная".
 *
 * <p>У первого прогона сравнивать не с чем: вызывающий код этот метод не вызывает и отдаёт null, а не нули.
 */
public final class IssueDeltaCalculator {

    private IssueDeltaCalculator() {
    }

    public static IssueDelta calculate(Collection<Issue> current, Collection<Issue> previous) {
        Set<IssueKey> currentKeys = keys(current);
        Set<IssueKey> previousKeys = keys(previous);

        List<Issue> newIssues = current.stream().filter(issue -> !previousKeys.contains(issue.key())).toList();
        List<Issue> persisting = current.stream().filter(issue -> previousKeys.contains(issue.key())).toList();
        List<Issue> resolved = previous.stream().filter(issue -> !currentKeys.contains(issue.key())).toList();
        return new IssueDelta(newIssues, persisting, resolved);
    }

    private static Set<IssueKey> keys(Collection<Issue> issues) {
        return issues.stream().map(Issue::key).collect(Collectors.toSet());
    }
}
