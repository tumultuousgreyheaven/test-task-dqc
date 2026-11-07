package com.example.dqc.infrastructure.persistence;

import com.example.dqc.application.repository.OrgUnitRepository;
import com.example.dqc.application.repository.SnapshotRepository;
import com.example.dqc.domain.model.Goal;
import com.example.dqc.domain.model.GoalStatus;
import com.example.dqc.domain.model.KeyResult;
import com.example.dqc.domain.model.OrgUnit;
import com.example.dqc.domain.model.QualitySnapshot;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Собирает снимок тремя запросами: подразделения, цели, КР. Грузит всё без фильтра по статусу:
 * цели нужны целиком, чтобы метрика каскада могла пройти вверх по цепочке родителей,
 * а отбор активных целей делает сам снимок.
 */
@Repository
public class JdbcSnapshotRepository implements SnapshotRepository {

    private static final String SELECT_GOALS = """
            SELECT id, unit_id, title, owner, parent_goal_id, status, due_date
            FROM goal
            ORDER BY id
            """;

    private static final String SELECT_KEY_RESULTS = """
            SELECT id, goal_id, title, target_value, actual_value, weight, fact_updated_at
            FROM key_result
            ORDER BY goal_id, id
            """;

    private static final RowMapper<Goal> GOAL_MAPPER = (rs, rowNum) -> new Goal(
            rs.getString("id"),
            rs.getString("unit_id"),
            rs.getString("title"),
            rs.getString("owner"),
            rs.getString("parent_goal_id"),
            GoalStatus.fromCode(rs.getString("status")),
            rs.getObject("due_date", LocalDate.class));

    private static final RowMapper<KeyResult> KEY_RESULT_MAPPER = (rs, rowNum) -> new KeyResult(
            rs.getString("id"),
            rs.getString("goal_id"),
            rs.getString("title"),
            rs.getBigDecimal("target_value"),
            rs.getBigDecimal("actual_value"),
            rs.getBigDecimal("weight"),
            rs.getObject("fact_updated_at", LocalDate.class));

    private final JdbcClient jdbc;
    private final OrgUnitRepository orgUnits;

    public JdbcSnapshotRepository(JdbcClient jdbc, OrgUnitRepository orgUnits) {
        this.jdbc = jdbc;
        this.orgUnits = orgUnits;
    }

    @Override
    public QualitySnapshot load(LocalDate asOf) {
        List<OrgUnit> units = orgUnits.findAll();
        List<Goal> goals = jdbc.sql(SELECT_GOALS).query(GOAL_MAPPER).list();
        List<KeyResult> keyResults = jdbc.sql(SELECT_KEY_RESULTS).query(KEY_RESULT_MAPPER).list();
        return new QualitySnapshot(asOf, units, goals, keyResults);
    }
}
