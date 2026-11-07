package com.example.dqc.application.service;

import com.example.dqc.application.exception.InvalidRequestException;
import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunListItem;
import com.example.dqc.application.repository.IssueRepository;
import com.example.dqc.application.repository.MetricValueRepository;
import com.example.dqc.application.repository.RunRepository;
import com.example.dqc.application.repository.SnapshotRepository;
import com.example.dqc.domain.metric.MetricCalculator;
import com.example.dqc.domain.metric.MetricValue;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.rule.RuleEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Запуск ККД: загрузить данные, применить правила, посчитать метрики, сохранить результат.
 *
 * <p>Весь прогон выполняется в одной транзакции. Поэтому при ошибке не остаётся ни прогона, ни
 * половины его результатов, а снаружи видны только прогоны в статусе completed.
 */
@Service
public class RunService {

    private final RunRepository runs;
    private final SnapshotRepository snapshots;
    private final IssueRepository issues;
    private final MetricValueRepository metricValues;
    private final RuleEngine ruleEngine;
    private final MetricCalculator metricCalculator;
    private final Clock clock;

    public RunService(RunRepository runs,
                      SnapshotRepository snapshots,
                      IssueRepository issues,
                      MetricValueRepository metricValues,
                      RuleEngine ruleEngine,
                      MetricCalculator metricCalculator,
                      Clock clock) {
        this.runs = runs;
        this.snapshots = snapshots;
        this.issues = issues;
        this.metricValues = metricValues;
        this.ruleEngine = ruleEngine;
        this.metricCalculator = metricCalculator;
        this.clock = clock;
    }

    /**
     * Запускает ККД по текущим данным. Прогон сравнивается с последним завершённым прогоном
     * со снимком строго раньше указанной даты: повторный запуск на ту же дату не сравнивается сам с собой.
     *
     * @param snapshotDate дата снимка; null — сегодня
     * @return созданный прогон и число найденных в нём проблем
     * @throws InvalidRequestException если дата в будущем или раньше последнего завершённого прогона
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public RunListItem start(LocalDate snapshotDate) {
        LocalDate date = snapshotDate != null ? snapshotDate : LocalDate.now(clock);
        validate(date);

        String previousRunId = runs.findLatestCompletedBefore(date).map(Run::id).orElse(null);
        Run run = runs.create(date, previousRunId);

        QualitySnapshot snapshot = snapshots.load(date);
        List<Issue> found = ruleEngine.run(snapshot);
        List<MetricValue> measured = metricCalculator.calculate(snapshot, found);

        issues.saveAll(run.id(), found);
        metricValues.saveAll(run.id(), measured);
        runs.markCompleted(run.id());
        return new RunListItem(run.completed(), found.size());
    }

    private void validate(LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today)) {
            throw new InvalidRequestException(
                    "Дата снимка %s в будущем (сегодня %s)".formatted(date, today));
        }
        runs.findLatestCompleted()
                .filter(latest -> date.isBefore(latest.snapshotDate()))
                .ifPresent(latest -> {
                    throw new InvalidRequestException(
                            "Дата снимка %s раньше последнего завершённого прогона %s (%s)"
                                    .formatted(date, latest.id(), latest.snapshotDate()));
                });
    }
}
