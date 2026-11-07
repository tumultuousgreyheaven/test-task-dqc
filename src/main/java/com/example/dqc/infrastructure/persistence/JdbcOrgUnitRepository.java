package com.example.dqc.infrastructure.persistence;

import com.example.dqc.application.repository.OrgUnitRepository;
import com.example.dqc.domain.model.OrgUnit;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcOrgUnitRepository implements OrgUnitRepository {

    private static final String SELECT_ALL = """
            SELECT id, name, parent_id
            FROM org_unit
            ORDER BY id
            """;

    private static final RowMapper<OrgUnit> ROW_MAPPER = (rs, rowNum) ->
            new OrgUnit(rs.getString("id"), rs.getString("name"), rs.getString("parent_id"));

    private final JdbcClient jdbc;

    public JdbcOrgUnitRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<OrgUnit> findAll() {
        return jdbc.sql(SELECT_ALL).query(ROW_MAPPER).list();
    }
}
