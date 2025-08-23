package com.fto.api.branches;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.List;

@Service
public class BranchService {
    private final NamedParameterJdbcTemplate jdbc;

    public BranchService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<BranchItem> list(String projectSlug, String projectToken, Integer windowDays, Integer limit) {
        String sql = """
            SELECT tr.branch AS name, COUNT(*) AS runs
            FROM test_runs tr
            JOIN projects p ON p.id = tr.project_id
            WHERE (
                (:projectSlug  IS NOT NULL AND p.name  = :projectSlug)
                OR (:projectToken IS NOT NULL AND p.token = :projectToken)
            )
            AND tr.branch IS NOT NULL AND tr.branch <> ''
            GROUP BY tr.branch
            ORDER BY runs DESC, name ASC
            LIMIT :limit
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("projectSlug",  projectSlug == null || projectSlug.isBlank() ? null : projectSlug, Types.VARCHAR)
                .addValue("projectToken", projectToken == null || projectToken.isBlank() ? null : projectToken, Types.VARCHAR)
                .addValue("windowDays",   windowDays, Types.INTEGER)
                .addValue("limit",        (limit == null || limit < 1) ? 100 : limit);

        return jdbc.query(sql, params, (rs, i) -> {
            BranchItem bi = new BranchItem();
            bi.name = rs.getString("name");
            bi.runs = rs.getInt("runs");
            return bi;
        });
    }
}
