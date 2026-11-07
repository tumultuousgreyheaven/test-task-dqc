package com.example.dqc.application.service;

import com.example.dqc.application.exception.ConflictException;
import com.example.dqc.application.exception.InvalidRequestException;
import com.example.dqc.application.exception.NotFoundException;
import com.example.dqc.application.model.DeltaCounts;
import com.example.dqc.application.model.IssueFilter;
import com.example.dqc.application.model.MetricSummary;
import com.example.dqc.application.model.QualitySummary;
import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunRef;
import com.example.dqc.application.model.SeveritySummary;
import com.example.dqc.application.model.UnitScope;
import com.example.dqc.application.repository.IssueRepository;
import com.example.dqc.application.repository.MetricValueRepository;
import com.example.dqc.application.repository.RunRepository;
import com.example.dqc.domain.delta.IssueCounts;
import com.example.dqc.domain.delta.IssueDelta;
import com.example.dqc.domain.delta.IssueDeltaCalculator;
import com.example.dqc.domain.metric.MetricComparator;
import com.example.dqc.domain.metric.MetricCalculator;
import com.example.dqc.domain.metric.MetricValue;
import com.example.dqc.domain.metric.QualityMetric;
import com.example.dqc.domain.metric.Ratio;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.Severity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Сводка ККД по подразделению: метрики с дельтой, проблемы по важности и счётчики новых/оставшихся/исправленных.
 * Все числа считаются по подразделению вместе со всеми дочерними.
 */
@Service
public class SummaryService {

    private final RunResolver runResolver;
    private final UnitScopeResolver unitResolver;
    private final RunRepository runs;
    private final IssueRepository issues;
    private final MetricValueRepository metricValues;
    private final MetricCalculator metricCalculator;

    SummaryService(RunResolver runResolver,
                   UnitScopeResolver unitResolver,
                   RunRepository runs,
                   IssueRepository issues,
                   MetricValueRepository metricValues,
                   MetricCalculator metricCalculator) {
        this.runResolver = runResolver;
        this.unitResolver = unitResolver;
        this.runs = runs;
        this.issues = issues;
        this.metricValues = metricValues;
        this.metricCalculator = metricCalculator;
    }

    /**
     * @param runId  прогон; null — последний завершённый
     * @param unitId подразделение (обязательно)
     * @throws InvalidRequestException если подразделение не указано
     * @throws NotFoundException       если нет такого прогона или подразделения
     * @throws ConflictException       если прогон ещё не завершён
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QualitySummary summarize(String runId, String unitId) {
        if (unitId == null || unitId.isBlank()) {
            throw new InvalidRequestException("unit_id обязателен");
        }
        Run run = runResolver.resolveCompleted(runId);
        UnitScope scope = unitResolver.resolve(unitId);
        Run previousRun = run.comparedToRunId() == null ? null : runs.findById(run.comparedToRunId())
                .orElseThrow(() -> new IllegalStateException(
                        "Прогон %s сравнивается с несуществующим прогоном %s".formatted(run.id(), run.comparedToRunId())));

        IssueFilter filter = new IssueFilter(scope.unitIds(), null, null);
        List<Issue> currentIssues = issues.find(run.id(), filter);
        List<Issue> previousIssues = previousRun == null ? null : issues.find(previousRun.id(), filter);

        return new QualitySummary(
                RunRef.of(run),
                previousRun == null ? null : RunRef.of(previousRun),
                scope.unit(),
                metrics(run, previousRun, scope.unitIds()),
                severities(currentIssues, previousIssues),
                delta(currentIssues, previousIssues));
    }

    private List<MetricSummary> metrics(Run run, Run previousRun, Set<String> unitIds) {
        Map<String, Ratio> current = totals(metricValues.findByRun(run.id()), unitIds);
        Map<String, Ratio> previous = previousRun == null
                ? Map.of()
                : totals(metricValues.findByRun(previousRun.id()), unitIds);

        List<MetricSummary> result = new ArrayList<>();
        for (QualityMetric metric : metricCalculator.metrics()) {
            // Нет строк = метрика в этом прогоне не считалась. previous тогда null (а не 0),
            // value у такой метрики тоже пустое, а не 0%.
            Ratio now = current.getOrDefault(metric.key(), Ratio.ZERO);
            Ratio before = previous.get(metric.key());
            result.add(new MetricSummary(metric.key(), metric.title(), metric.unit(),
                    MetricComparator.compare(now, before, metric.higherIsBetter())));
        }
        return result;
    }

    /** Складывает собственные доли подразделений выбранного поддерева по каждой метрике. */
    private static Map<String, Ratio> totals(Collection<MetricValue> values, Set<String> unitIds) {
        Map<String, Ratio> totals = new HashMap<>();
        for (MetricValue value : values) {
            if (unitIds.contains(value.unitId())) {
                totals.merge(value.metricKey(), value.ratio(), Ratio::plus);
            }
        }
        return totals;
    }

    private static List<SeveritySummary> severities(List<Issue> current, List<Issue> previous) {
        Map<Severity, Integer> now = IssueCounts.bySeverity(current);
        Map<Severity, Integer> before = previous == null ? null : IssueCounts.bySeverity(previous);
        List<SeveritySummary> result = new ArrayList<>();
        for (Severity severity : Severity.values()) {
            int count = now.get(severity);
            Integer previousCount = before == null ? null : before.get(severity);
            result.add(new SeveritySummary(severity, count, previousCount,
                    previousCount == null ? null : count - previousCount));
        }
        return result;
    }

    private static DeltaCounts delta(List<Issue> current, List<Issue> previous) {
        if (previous == null) {
            return null;
        }
        IssueDelta delta = IssueDeltaCalculator.calculate(current, previous);
        return new DeltaCounts(delta.newIssues().size(), delta.persisting().size(), delta.resolved().size());
    }
}
