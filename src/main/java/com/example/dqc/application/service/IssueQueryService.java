package com.example.dqc.application.service;

import com.example.dqc.application.exception.ConflictException;
import com.example.dqc.application.exception.InvalidRequestException;
import com.example.dqc.application.exception.NotFoundException;
import com.example.dqc.application.model.IssueFilter;
import com.example.dqc.application.model.IssuePage;
import com.example.dqc.application.model.IssueSearchRequest;
import com.example.dqc.application.model.PageResult;
import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RuleDefinition;
import com.example.dqc.application.repository.IssueRepository;
import com.example.dqc.application.repository.RuleCatalogRepository;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.Severity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Поиск проблем прогона с фильтрами и постраничной выдачей. */
@Service
public class IssueQueryService {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private final RunResolver runResolver;
    private final UnitScopeResolver unitResolver;
    private final IssueRepository issues;
    private final RuleCatalogRepository ruleCatalog;

    IssueQueryService(RunResolver runResolver,
                      UnitScopeResolver unitResolver,
                      IssueRepository issues,
                      RuleCatalogRepository ruleCatalog) {
        this.runResolver = runResolver;
        this.unitResolver = unitResolver;
        this.issues = issues;
        this.ruleCatalog = ruleCatalog;
    }

    /**
     * Сначала проверяются сами параметры (422), потом существование прогона и подразделения (404, 409).
     *
     * @throws InvalidRequestException если limit, offset, важность или правило некорректны
     * @throws NotFoundException       если нет такого прогона или подразделения
     * @throws ConflictException       если прогон ещё не завершён
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public IssuePage search(IssueSearchRequest request) {
        int limit = limit(request.limit());
        int offset = offset(request.offset());
        Severity severity = severity(request.severity());
        String rule = rule(request.rule());
        if (request.unitId() != null && request.unitId().isBlank()) {
            throw new InvalidRequestException("unit_id не должен быть пустым");
        }

        Run run = runResolver.resolveCompleted(request.runId());
        Set<String> unitIds = request.unitId() == null ? null : unitResolver.resolve(request.unitId()).unitIds();

        PageResult<Issue> page = issues.findPage(run.id(), new IssueFilter(unitIds, rule, severity), limit, offset);
        return new IssuePage(page.items(), page.total(), limit, offset);
    }

    private static int limit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidRequestException("limit должен быть от 1 до %d, получено %d".formatted(MAX_LIMIT, limit));
        }
        return limit;
    }

    private static int offset(Integer offset) {
        if (offset == null) {
            return 0;
        }
        if (offset < 0) {
            throw new InvalidRequestException("offset не может быть отрицательным, получено " + offset);
        }
        return offset;
    }

    private static Severity severity(String code) {
        if (code == null) {
            return null;
        }
        try {
            return Severity.fromCode(code);
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(Severity.values()).map(Severity::code).collect(Collectors.joining(", "));
            throw new InvalidRequestException("Недопустимая важность: %s (допустимо: %s)".formatted(code, allowed));
        }
    }

    /** Код правила проверяется по справочнику, включая выключенные правила: их старые проблемы остаются в прогонах. */
    private String rule(String code) {
        if (code == null) {
            return null;
        }
        boolean known = ruleCatalog.findAll().stream().map(RuleDefinition::code).anyMatch(code::equals);
        if (!known) {
            throw new InvalidRequestException("Неизвестное правило: " + code);
        }
        return code;
    }
}
