package com.example.dqc.infrastructure.persistence;

import com.example.dqc.application.model.RuleDefinition;
import com.example.dqc.application.repository.RuleCatalogRepository;
import com.example.dqc.domain.model.Severity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcRuleCatalogRepository implements RuleCatalogRepository {

    private static final String SELECT_ALL = """
            SELECT code, title, severity, is_active
            FROM kkd_rule
            ORDER BY code
            """;

    private static final RowMapper<RuleDefinition> ROW_MAPPER = (rs, rowNum) -> new RuleDefinition(
            rs.getString("code"),
            rs.getString("title"),
            Severity.fromCode(rs.getString("severity")),
            rs.getBoolean("is_active"));

    private final JdbcClient jdbc;

    public JdbcRuleCatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RuleDefinition> findAll() {
        return jdbc.sql(SELECT_ALL).query(ROW_MAPPER).list();
    }
}
