package com.example.dqc.application.service;

import com.example.dqc.application.exception.InvalidRequestException;
import com.example.dqc.application.model.IssueFilter;
import com.example.dqc.application.model.IssueSearchRequest;
import com.example.dqc.application.model.PageResult;
import com.example.dqc.application.model.RuleDefinition;
import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunStatus;
import com.example.dqc.application.repository.IssueRepository;
import com.example.dqc.application.repository.OrgUnitRepository;
import com.example.dqc.application.repository.RuleCatalogRepository;
import com.example.dqc.application.repository.RunRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueQueryServiceTest {

    @Mock
    private RunRepository runs;
    @Mock
    private OrgUnitRepository units;
    @Mock
    private IssueRepository issues;
    @Mock
    private RuleCatalogRepository ruleCatalog;

    private IssueQueryService service;

    @BeforeEach
    void setUp() {
        service = new IssueQueryService(new RunResolver(runs), new UnitScopeResolver(units), issues, ruleCatalog);
    }

    @Test
    void validatesFiltersBeforeLookingUpRun() {
        IssueSearchRequest request = new IssueSearchRequest("RUN-UNKNOWN", null, null, "urgent", null, null);

        assertThatThrownBy(() -> service.search(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Недопустимая важность");

        verify(runs, never()).findById("RUN-UNKNOWN");
        verify(runs, never()).findLatestCompleted();
    }

    @Test
    void validatesUnknownRuleBeforeLookingUpRun() {
        when(ruleCatalog.findAll()).thenReturn(List.of());
        IssueSearchRequest request = new IssueSearchRequest("RUN-UNKNOWN", null, "UNKNOWN_RULE", null, null, null);

        assertThatThrownBy(() -> service.search(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Неизвестное правило");

        verify(runs, never()).findById("RUN-UNKNOWN");
        verify(runs, never()).findLatestCompleted();
    }

    @Test
    void appliesDefaultsAndFiltersByUnitSubtreeIncludingInactiveCatalogRules() {
        stubLatestRun();
        stubUnits();
        when(ruleCatalog.findAll()).thenReturn(List.of(
                new RuleDefinition("GOAL_NO_OWNER", "No owner", Severity.CRITICAL, true),
                new RuleDefinition("KR_NO_TARGET", "No target", Severity.MAJOR, false)));
        when(issues.findPage(eq("RUN-1"), any(IssueFilter.class), eq(50), eq(0)))
                .thenReturn(new PageResult<>(List.of(), 12));

        var page = service.search(new IssueSearchRequest(null, "HQ", "KR_NO_TARGET", "major", null, null));

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isEqualTo(12);
        assertThat(page.limit()).isEqualTo(50);
        assertThat(page.offset()).isZero();

        ArgumentCaptor<IssueFilter> filterCaptor = ArgumentCaptor.forClass(IssueFilter.class);
        verify(issues).findPage(eq("RUN-1"), filterCaptor.capture(), eq(50), eq(0));
        assertThat(filterCaptor.getValue().unitIds()).containsExactlyInAnyOrder("HQ", "TEAM-A", "TEAM-B");
        assertThat(filterCaptor.getValue().ruleCode()).isEqualTo("KR_NO_TARGET");
        assertThat(filterCaptor.getValue().severity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    void passesExplicitPaginationValuesToRepository() {
        when(runs.findById("RUN-1")).thenReturn(Optional.of(
                new Run("RUN-1", LocalDate.of(2026, 11, 7), RunStatus.COMPLETED, null)));
        when(issues.findPage(eq("RUN-1"), any(IssueFilter.class), eq(10), eq(25)))
                .thenReturn(new PageResult<>(List.of(), 30));

        var page = service.search(new IssueSearchRequest("RUN-1", null, null, null, 10, 25));

        assertThat(page.total()).isEqualTo(30);
        assertThat(page.limit()).isEqualTo(10);
        assertThat(page.offset()).isEqualTo(25);
        verify(runs).findById("RUN-1");
        verify(runs, never()).findLatestCompleted();
        verify(issues).findPage(eq("RUN-1"), eq(IssueFilter.all()), eq(10), eq(25));
    }

    @Test
    void rejectsBlankOptionalUnitIdAsInvalidParameter() {
        IssueSearchRequest request = new IssueSearchRequest("RUN-UNKNOWN", "   ", null, null, null, null);

        assertThatThrownBy(() -> service.search(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("unit_id не должен быть пустым");
        verify(runs, never()).findById("RUN-UNKNOWN");
    }

    @Test
    void rejectsOutOfRangePagination() {
        assertThatThrownBy(() -> service.search(new IssueSearchRequest(null, null, null, null, 0, null)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("limit");
        assertThatThrownBy(() -> service.search(new IssueSearchRequest(null, null, null, null, 201, null)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("limit");
        assertThatThrownBy(() -> service.search(new IssueSearchRequest(null, null, null, null, null, -1)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("offset");
        verify(runs, never()).findLatestCompleted();
    }

    private void stubLatestRun() {
        when(runs.findLatestCompleted()).thenReturn(Optional.of(
                new Run("RUN-1", LocalDate.of(2026, 11, 7), RunStatus.COMPLETED, null)));
    }

    private void stubUnits() {
        when(units.findAll()).thenReturn(List.of(
                new OrgUnit("HQ", "Head office", null),
                new OrgUnit("TEAM-A", "Team A", "HQ"),
                new OrgUnit("TEAM-B", "Team B", "HQ")));
    }
}
