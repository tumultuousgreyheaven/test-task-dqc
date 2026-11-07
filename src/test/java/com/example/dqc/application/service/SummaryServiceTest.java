package com.example.dqc.application.service;

import com.example.dqc.application.model.IssueFilter;
import com.example.dqc.application.exception.InvalidRequestException;
import com.example.dqc.application.exception.NotFoundException;
import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunStatus;
import com.example.dqc.application.repository.IssueRepository;
import com.example.dqc.application.repository.MetricValueRepository;
import com.example.dqc.application.repository.OrgUnitRepository;
import com.example.dqc.application.repository.RunRepository;
import com.example.dqc.domain.metric.CleanGoalsShareMetric;
import com.example.dqc.domain.metric.MetricCalculator;
import com.example.dqc.domain.metric.MetricValue;
import com.example.dqc.domain.metric.Ratio;
import com.example.dqc.domain.metric.Trend;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.ObjectType;
import com.example.dqc.domain.model.OrgUnit;
import com.example.dqc.domain.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private RunRepository runs;
    @Mock
    private OrgUnitRepository units;
    @Mock
    private IssueRepository issues;
    @Mock
    private MetricValueRepository metricValues;

    private SummaryService service;

    @BeforeEach
    void setUp() {
        service = new SummaryService(new RunResolver(runs), new UnitScopeResolver(units),
                runs, issues, metricValues, new MetricCalculator(List.of(new CleanGoalsShareMetric())));
    }

    @Test
    void aggregatesRatiosAcrossSubtreeAndCalculatesIssueAndMetricDeltas() {
        stubUnits();
        Run currentRun = run("RUN-2", "RUN-1", RunStatus.COMPLETED, "2026-11-07");
        Run previousRun = run("RUN-1", null, RunStatus.COMPLETED, "2026-10-31");
        when(runs.findById("RUN-2")).thenReturn(java.util.Optional.of(currentRun));
        when(runs.findById("RUN-1")).thenReturn(java.util.Optional.of(previousRun));

        List<Issue> currentIssues = List.of(
                issue("GOAL_NO_OWNER", Severity.MAJOR, ObjectType.GOAL, "G-1", "G-1", "HQ"),
                issue("KR_NO_TARGET", Severity.MAJOR, ObjectType.KEY_RESULT, "KR-2", "G-2", "TEAM-A"));
        List<Issue> previousIssues = List.of(
                // Same key despite the changed severity and message: this issue persists.
                issue("GOAL_NO_OWNER", Severity.CRITICAL, ObjectType.GOAL, "G-1", "G-1", "HQ"),
                issue("GOAL_NO_KR", Severity.CRITICAL, ObjectType.GOAL, "G-OLD", "G-OLD", "TEAM-B"));
        when(issues.find(eq("RUN-2"), any(IssueFilter.class))).thenReturn(currentIssues);
        when(issues.find(eq("RUN-1"), any(IssueFilter.class))).thenReturn(previousIssues);

        when(metricValues.findByRun("RUN-2")).thenReturn(List.of(
                metric("HQ", 0, 1), metric("TEAM-A", 1, 1), metric("TEAM-B", 0, 3), metric("OTHER", 1, 1)));
        when(metricValues.findByRun("RUN-1")).thenReturn(List.of(
                metric("HQ", 1, 1), metric("TEAM-A", 0, 1), metric("TEAM-B", 1, 1), metric("OTHER", 0, 1)));

        var summary = service.summarize("RUN-2", "HQ");

        assertThat(summary.run().id()).isEqualTo("RUN-2");
        assertThat(summary.comparedTo().id()).isEqualTo("RUN-1");
        assertThat(summary.unit().id()).isEqualTo("HQ");
        assertThat(summary.metrics()).singleElement().satisfies(metric -> {
            assertThat(metric.key()).isEqualTo(CleanGoalsShareMetric.KEY);
            assertThat(metric.reading().value()).isEqualByComparingTo("20.0"); // (0 + 1 + 0) / (1 + 1 + 3)
            assertThat(metric.reading().previous()).isEqualByComparingTo("66.7"); // (1 + 0 + 1) / (1 + 1 + 1)
            assertThat(metric.reading().delta()).isEqualByComparingTo("-46.7");
            assertThat(metric.reading().trend()).isEqualTo(Trend.WORSE);
        });

        assertThat(summary.issuesBySeverity()).hasSize(3);
        assertThat(summary.issuesBySeverity().get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(summary.issuesBySeverity().get(0).current()).isZero();
        assertThat(summary.issuesBySeverity().get(0).previous()).isEqualTo(2);
        assertThat(summary.issuesBySeverity().get(0).delta()).isEqualTo(-2);
        assertThat(summary.issuesBySeverity().get(1).current()).isEqualTo(2);
        assertThat(summary.issuesBySeverity().get(1).previous()).isZero();
        assertThat(summary.issuesBySeverity().get(1).delta()).isEqualTo(2);
        assertThat(summary.issuesBySeverity().get(2).current()).isZero();

        assertThat(summary.delta().newIssues()).isEqualTo(1);
        assertThat(summary.delta().persisting()).isEqualTo(1);
        assertThat(summary.delta().resolved()).isEqualTo(1);

        ArgumentCaptor<IssueFilter> filterCaptor = ArgumentCaptor.forClass(IssueFilter.class);
        verify(issues).find(eq("RUN-2"), filterCaptor.capture());
        assertThat(filterCaptor.getValue().unitIds()).containsExactlyInAnyOrder("HQ", "TEAM-A", "TEAM-B");
    }

    @Test
    void rejectsMissingUnitIdAsInvalidParameter() {
        assertThatThrownBy(() -> service.summarize("RUN-1", " "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("unit_id обязателен");
    }

    @Test
    void returnsNotFoundWhenUnitDoesNotExist() {
        stubUnits();
        when(runs.findById("RUN-1")).thenReturn(java.util.Optional.of(
                run("RUN-1", null, RunStatus.COMPLETED, "2026-11-07")));

        assertThatThrownBy(() -> service.summarize("RUN-1", "UNKNOWN"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Подразделение не найдено");
    }

    @Test
    void firstRunHasNullComparisonFieldsInsteadOfZeroes() {
        stubUnits();
        Run firstRun = run("RUN-1", null, RunStatus.COMPLETED, "2026-11-07");
        when(runs.findById("RUN-1")).thenReturn(java.util.Optional.of(firstRun));
        when(issues.find(eq("RUN-1"), any(IssueFilter.class))).thenReturn(List.of(
                issue("GOAL_NO_OWNER", Severity.CRITICAL, ObjectType.GOAL, "G-1", "G-1", "HQ")));
        when(metricValues.findByRun("RUN-1")).thenReturn(List.of(metric("HQ", 1, 2)));

        var summary = service.summarize("RUN-1", "HQ");

        assertThat(summary.comparedTo()).isNull();
        assertThat(summary.delta()).isNull();
        assertThat(summary.metrics()).singleElement().satisfies(metric -> {
            assertThat(metric.reading().value()).isEqualByComparingTo("50.0");
            assertThat(metric.reading().previous()).isNull();
            assertThat(metric.reading().delta()).isNull();
            assertThat(metric.reading().trend()).isNull();
        });
        assertThat(summary.issuesBySeverity()).allSatisfy(severity -> {
            assertThat(severity.previous()).isNull();
            assertThat(severity.delta()).isNull();
        });
        verify(metricValues).findByRun("RUN-1");
    }

    private void stubUnits() {
        when(units.findAll()).thenReturn(List.of(
                new OrgUnit("HQ", "Head office", null),
                new OrgUnit("TEAM-A", "Team A", "HQ"),
                new OrgUnit("TEAM-B", "Team B", "HQ"),
                new OrgUnit("OTHER", "Outside", null)));
    }

    private static Run run(String id, String previousId, RunStatus status, String date) {
        return new Run(id, LocalDate.parse(date), status, previousId);
    }

    private static MetricValue metric(String unitId, int numerator, int denominator) {
        return new MetricValue(CleanGoalsShareMetric.KEY, unitId, new Ratio(numerator, denominator));
    }

    private static Issue issue(String rule, Severity severity, ObjectType type,
                               String objectId, String goalId, String unitId) {
        return new Issue(rule, severity, type, objectId, goalId, unitId, rule + " message");
    }
}
