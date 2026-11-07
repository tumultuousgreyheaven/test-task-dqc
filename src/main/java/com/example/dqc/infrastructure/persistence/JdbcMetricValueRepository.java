package com.example.dqc.infrastructure.persistence;

import com.example.dqc.application.repository.MetricValueRepository;
import com.example.dqc.domain.metric.MetricValue;
import com.example.dqc.domain.metric.Ratio;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class JdbcMetricValueRepository implements MetricValueRepository {

    private static final String INSERT = """
            INSERT INTO kkd_metric_value (run_id, unit_id, metric_key, numerator, denominator)
            VALUES (:runId, :unitId, :metricKey, :numerator, :denominator)
            """;

    private static final String SELECT_BY_RUN = """
            SELECT unit_id, metric_key, numerator, denominator
            FROM kkd_metric_value
            WHERE run_id = :runId
            ORDER BY metric_key, unit_id
            """;

    private static final RowMapper<MetricValue> ROW_MAPPER = (rs, rowNum) -> new MetricValue(
            rs.getString("metric_key"),
            rs.getString("unit_id"),
            new Ratio(rs.getInt("numerator"), rs.getInt("denominator")));

    private final JdbcClient jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public JdbcMetricValueRepository(JdbcClient jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public void saveAll(String runId, Collection<MetricValue> values) {
        if (values.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = values.stream()
                .map(value -> new MapSqlParameterSource()
                        .addValue("runId", runId)
                        .addValue("unitId", value.unitId())
                        .addValue("metricKey", value.metricKey())
                        .addValue("numerator", value.ratio().numerator())
                        .addValue("denominator", value.ratio().denominator()))
                .toArray(SqlParameterSource[]::new);
        namedJdbc.batchUpdate(INSERT, batch);
    }

    @Override
    public List<MetricValue> findByRun(String runId) {
        return jdbc.sql(SELECT_BY_RUN).param("runId", runId).query(ROW_MAPPER).list();
    }
}
