package com.example.dqc.infrastructure.persistence;

import com.example.dqc.application.model.Run;
import com.example.dqc.application.model.RunListItem;
import com.example.dqc.application.model.RunStatus;
import com.example.dqc.application.repository.RunRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Порядок "новые первыми": дата снимка, затем время запуска, затем номер прогона.
 * Номер сравнивается сначала по длине, чтобы RUN-1000 шёл после RUN-999, а не перед ним.
 */
@Repository
public class JdbcRunRepository implements RunRepository {

    private static final String INSERT_RUN = """
            INSERT INTO kkd_run (id, snapshot_date, status, compared_to_run_id)
            VALUES ('RUN-' || lpad(nextval('kkd_run_seq')::text, 3, '0'),
                    :snapshotDate, 'running', CAST(:comparedTo AS varchar))
            RETURNING id
            """;

    private static final String MARK_COMPLETED = """
            UPDATE kkd_run
            SET status = 'completed', finished_at = now()
            WHERE id = :id
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, snapshot_date, status, compared_to_run_id
            FROM kkd_run
            WHERE id = :id
            """;

    private static final String SELECT_LATEST_COMPLETED = """
            SELECT id, snapshot_date, status, compared_to_run_id
            FROM kkd_run
            WHERE status = 'completed'
            ORDER BY snapshot_date DESC, started_at DESC, length(id) DESC, id DESC
            LIMIT 1
            """;

    private static final String SELECT_LATEST_COMPLETED_BEFORE = """
            SELECT id, snapshot_date, status, compared_to_run_id
            FROM kkd_run
            WHERE status = 'completed' AND snapshot_date < :snapshotDate
            ORDER BY snapshot_date DESC, started_at DESC, length(id) DESC, id DESC
            LIMIT 1
            """;

    private static final String SELECT_ALL_WITH_ISSUE_COUNT = """
            SELECT r.id, r.snapshot_date, r.status, r.compared_to_run_id,
                   (SELECT count(*) FROM kkd_issue i WHERE i.run_id = r.id) AS issue_count
            FROM kkd_run r
            ORDER BY r.snapshot_date DESC, r.started_at DESC, length(r.id) DESC, r.id DESC
            """;

    private static final RowMapper<Run> RUN_MAPPER = (rs, rowNum) -> new Run(
            rs.getString("id"),
            rs.getObject("snapshot_date", LocalDate.class),
            RunStatus.fromCode(rs.getString("status")),
            rs.getString("compared_to_run_id"));

    private static final RowMapper<RunListItem> RUN_LIST_ITEM_MAPPER = (rs, rowNum) ->
            new RunListItem(RUN_MAPPER.mapRow(rs, rowNum), rs.getInt("issue_count"));

    private final JdbcClient jdbc;

    public JdbcRunRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Run create(LocalDate snapshotDate, String comparedToRunId) {
        String id = jdbc.sql(INSERT_RUN)
                .param("snapshotDate", snapshotDate)
                .param("comparedTo", comparedToRunId)
                .query(String.class)
                .single();
        return new Run(id, snapshotDate, RunStatus.RUNNING, comparedToRunId);
    }

    @Override
    public void markCompleted(String runId) {
        int updated = jdbc.sql(MARK_COMPLETED).param("id", runId).update();
        if (updated != 1) {
            throw new IllegalStateException("Не удалось завершить прогон, его нет в БД: " + runId);
        }
    }

    @Override
    public Optional<Run> findById(String runId) {
        return jdbc.sql(SELECT_BY_ID).param("id", runId).query(RUN_MAPPER).optional();
    }

    @Override
    public Optional<Run> findLatestCompleted() {
        return jdbc.sql(SELECT_LATEST_COMPLETED).query(RUN_MAPPER).optional();
    }

    @Override
    public Optional<Run> findLatestCompletedBefore(LocalDate snapshotDate) {
        return jdbc.sql(SELECT_LATEST_COMPLETED_BEFORE)
                .param("snapshotDate", snapshotDate)
                .query(RUN_MAPPER)
                .optional();
    }

    @Override
    public List<RunListItem> findAllWithIssueCount() {
        return jdbc.sql(SELECT_ALL_WITH_ISSUE_COUNT).query(RUN_LIST_ITEM_MAPPER).list();
    }
}
