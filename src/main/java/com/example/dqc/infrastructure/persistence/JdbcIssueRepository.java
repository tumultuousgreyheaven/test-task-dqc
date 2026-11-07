package com.example.dqc.infrastructure.persistence;

import com.example.dqc.application.model.IssueFilter;
import com.example.dqc.application.model.PageResult;
import com.example.dqc.application.repository.IssueRepository;
import com.example.dqc.domain.model.Issue;
import com.example.dqc.domain.model.ObjectType;
import com.example.dqc.domain.model.Severity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Запросы собираются из фрагментов: основа + необходимые условия AND + порядок (+ страница).
 * В условия подставляются только именованные параметры, текст запроса от значений не зависит.
 */
@Repository
public class JdbcIssueRepository implements IssueRepository {

    private static final String INSERT = """
            INSERT INTO kkd_issue (run_id, rule_code, severity, object_type, object_id, goal_id, unit_id, message)
            VALUES (:runId, :ruleCode, :severity, :objectType, :objectId, :goalId, :unitId, :message)
            """;

    private static final String SELECT = """
            SELECT i.rule_code, i.severity, i.object_type, i.object_id, i.goal_id, i.unit_id, i.message
            FROM kkd_issue i
            WHERE i.run_id = :runId
            """;

    private static final String COUNT = """
            SELECT count(*)
            FROM kkd_issue i
            WHERE i.run_id = :runId
            """;

    private static final String AND_UNITS = """
            AND i.unit_id IN (:unitIds)
            """;

    private static final String AND_RULE = """
            AND i.rule_code = :ruleCode
            """;

    private static final String AND_SEVERITY = """
            AND i.severity = :severity
            """;

    private static final String ORDER_BY = """
            ORDER BY CASE i.severity WHEN 'critical' THEN 0 WHEN 'major' THEN 1 ELSE 2 END,
                     i.rule_code, i.object_type, i.object_id, i.id
            """;

    private static final String PAGE = """
            LIMIT :limit OFFSET :offset
            """;

    private static final RowMapper<Issue> ROW_MAPPER = (rs, rowNum) -> new Issue(
            rs.getString("rule_code"),
            Severity.fromCode(rs.getString("severity")),
            ObjectType.fromCode(rs.getString("object_type")),
            rs.getString("object_id"),
            rs.getString("goal_id"),
            rs.getString("unit_id"),
            rs.getString("message"));

    private final JdbcClient jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public JdbcIssueRepository(JdbcClient jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public void saveAll(String runId, Collection<Issue> issues) {
        if (issues.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = issues.stream()
                .map(issue -> new MapSqlParameterSource()
                        .addValue("runId", runId)
                        .addValue("ruleCode", issue.ruleCode())
                        .addValue("severity", issue.severity().code())
                        .addValue("objectType", issue.objectType().code())
                        .addValue("objectId", issue.objectId())
                        .addValue("goalId", issue.goalId())
                        .addValue("unitId", issue.unitId())
                        .addValue("message", issue.message()))
                .toArray(SqlParameterSource[]::new);
        namedJdbc.batchUpdate(INSERT, batch);
    }

    @Override
    public List<Issue> find(String runId, IssueFilter filter) {
        if (filter.matchesNothing()) {
            return List.of();
        }
        Map<String, Object> params = new HashMap<>();
        StringBuilder sql = new StringBuilder(SELECT);
        appendConditions(sql, params, runId, filter);
        sql.append(ORDER_BY);
        return jdbc.sql(sql.toString()).params(params).query(ROW_MAPPER).list();
    }

    @Override
    public PageResult<Issue> findPage(String runId, IssueFilter filter, int limit, int offset) {
        if (filter.matchesNothing()) {
            return new PageResult<>(List.of(), 0);
        }
        Map<String, Object> params = new HashMap<>();
        StringBuilder countSql = new StringBuilder(COUNT);
        appendConditions(countSql, params, runId, filter);
        long total = jdbc.sql(countSql.toString()).params(params).query(Long.class).single();

        StringBuilder pageSql = new StringBuilder(SELECT);
        appendConditions(pageSql, params, runId, filter);
        pageSql.append(ORDER_BY).append(PAGE);
        params.put("limit", limit);
        params.put("offset", offset);
        List<Issue> items = jdbc.sql(pageSql.toString()).params(params).query(ROW_MAPPER).list();
        return new PageResult<>(items, total);
    }

    /** Добавляет к запросу условия фильтра и кладёт их значения в params. */
    private static void appendConditions(StringBuilder sql, Map<String, Object> params, String runId, IssueFilter filter) {
        params.put("runId", runId);
        if (filter.unitIds() != null) {
            sql.append(AND_UNITS);
            params.put("unitIds", filter.unitIds());
        }
        if (filter.ruleCode() != null) {
            sql.append(AND_RULE);
            params.put("ruleCode", filter.ruleCode());
        }
        if (filter.severity() != null) {
            sql.append(AND_SEVERITY);
            params.put("severity", filter.severity().code());
        }
    }
}
