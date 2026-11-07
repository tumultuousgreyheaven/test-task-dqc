package com.example.dqc.application.service;

import com.example.dqc.application.exception.InvalidRequestException;
import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunListItem;
import com.example.dqc.application.model.RunStatus;
import com.example.dqc.application.repository.IssueRepository;
import com.example.dqc.application.repository.MetricValueRepository;
import com.example.dqc.application.repository.RunRepository;
import com.example.dqc.application.repository.SnapshotRepository;
import com.example.dqc.domain.metric.CleanGoalsShareMetric;
import com.example.dqc.domain.metric.MetricCalculator;
import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.GoalStatus;
import com.example.dqc.domain.model.OrgUnit;
import com.example.dqc.domain.model.QualitySnapshot;
import com.example.dqc.domain.rule.GoalNoOwnerRule;
import com.example.dqc.domain.rule.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 7);

    @Mock
    private RunRepository runs;
    @Mock
    private SnapshotRepository snapshots;
    @Mock
    private IssueRepository issues;
    @Mock
    private MetricValueRepository metricValues;

    private RunService service;
    private QualitySnapshot snapshot;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC);
        service = new RunService(runs, snapshots, issues, metricValues,
                new RuleEngine(List.of(new GoalNoOwnerRule())),
                new MetricCalculator(List.of(new CleanGoalsShareMetric())), clock);
        snapshot = new QualitySnapshot(TODAY,
                List.of(new OrgUnit("U", "Unit", null)),
                List.of(new Goal("G-1", "U", "Goal", null, null, GoalStatus.ACTIVE, null)),
                List.of());
    }

    @Test
    void createsRunStoresIssuesAndMetricsAndMarksItCompleted() {
        stubLatestRun();
        when(runs.findLatestCompletedBefore(TODAY)).thenReturn(Optional.empty());
        when(runs.create(TODAY, null)).thenReturn(new Run("RUN-8", TODAY, RunStatus.RUNNING, null));
        when(snapshots.load(TODAY)).thenReturn(snapshot);

        RunListItem result = service.start(null);

        assertThat(result.run().id()).isEqualTo("RUN-8");
        assertThat(result.run().status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(result.issueCount()).isEqualTo(1);
        verify(issues).saveAll(eq("RUN-8"), any());
        verify(metricValues).saveAll(eq("RUN-8"), any());
        verify(runs).markCompleted("RUN-8");
    }

    @Test
    void comparesOnlyWithRunWhoseSnapshotDateIsStrictlyEarlier() {
        stubLatestRun();
        when(runs.findLatestCompletedBefore(TODAY)).thenReturn(Optional.of(
                new Run("RUN-6", TODAY.minusDays(7), RunStatus.COMPLETED, null)));
        when(runs.create(TODAY, "RUN-6")).thenReturn(
                new Run("RUN-8", TODAY, RunStatus.RUNNING, "RUN-6"));
        when(snapshots.load(TODAY)).thenReturn(snapshot);

        RunListItem result = service.start(TODAY);

        assertThat(result.run().comparedToRunId()).isEqualTo("RUN-6");
        verify(runs).create(TODAY, "RUN-6");
    }

    @Test
    void rejectsFutureSnapshotDateBeforeCreatingRun() {
        assertThatThrownBy(() -> service.start(TODAY.plusDays(1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("в будущем");

        verify(runs, never()).create(any(LocalDate.class), any());
        verify(snapshots, never()).load(any(LocalDate.class));
    }

    @Test
    void rejectsSnapshotDateEarlierThanLatestCompletedRun() {
        stubLatestRun();
        assertThatThrownBy(() -> service.start(TODAY.minusDays(1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("раньше последнего завершённого");

        verify(runs, never()).create(any(LocalDate.class), any());
        verify(snapshots, never()).load(any(LocalDate.class));
    }

    private void stubLatestRun() {
        when(runs.findLatestCompleted()).thenReturn(Optional.of(
                new Run("RUN-7", TODAY, RunStatus.COMPLETED, null)));
    }
}
