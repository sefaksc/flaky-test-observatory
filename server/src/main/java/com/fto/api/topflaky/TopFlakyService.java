package com.fto.api.topflaky;


import com.fto.api.topflaky.dto.TopFlakyItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Service
public class TopFlakyService {
    private final NamedParameterJdbcTemplate jdbc;


    public TopFlakyService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TopFlakyItem> query(String projectSlug, String projectToken, int windowDays, int minRuns, int sizePlusOne, int offset) {
        String sql = """
                WITH windowed AS (
                    SELECT
                        tr.id AS run_id,
                        tr.started_at AS run_started_at,
                        r.case_id       AS test_case_id,
                        tc.suite        AS class_name,
                        tc.name         AS test_name,
                        r.status
                    FROM test_runs tr
                    JOIN projects p     ON p.id = tr.project_id
                    JOIN test_results r ON r.run_id = tr.id
                    JOIN test_cases tc  ON tc.id = r.case_id
                    WHERE (
                        -- NOT: Artık p.slug YOK; proje parametresini p.name ile eşliyoruz
                        (:projectSlug   IS NOT NULL AND p.name  = :projectSlug)
                        OR
                        (:projectToken  IS NOT NULL AND p.token = :projectToken)
                    )
                    AND tr.started_at >= NOW() - (:windowDays || ' days')::interval
                ), ordered AS (
                    SELECT
                        test_case_id,
                        class_name,
                        test_name,
                        run_started_at,
                        status,
                        LAG(status) OVER (PARTITION BY test_case_id ORDER BY run_started_at) AS prev_status
                    FROM windowed
                ), agg AS (
                    SELECT
                        test_case_id,
                        MAX(class_name) AS class_name,
                        MAX(test_name)  AS test_name,
                        COUNT(*) AS runs,
                        COUNT(*) FILTER (WHERE status = 'FAILED') AS fails,
                        COUNT(*) FILTER (WHERE status = 'PASSED') AS passes,
                        COUNT(*) FILTER (WHERE prev_status IS NOT NULL AND status <> prev_status) AS flips,
                        MAX(run_started_at) AS last_seen_at
                    FROM ordered
                    GROUP BY test_case_id
                )
                SELECT
                    test_case_id,
                    class_name,
                    test_name,
                    runs,
                    fails,
                    passes,
                    CASE WHEN runs = 0 THEN 0 ELSE (fails::double precision / runs::double precision) END AS fail_rate,
                    flips,
                    last_seen_at
                FROM agg
                WHERE runs >= :minRuns AND fails > 0 AND passes > 0
                ORDER BY flips DESC, fail_rate DESC, runs DESC, last_seen_at DESC
                OFFSET :offset LIMIT :limit   
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("projectSlug",  isBlank(projectSlug)  ? null : projectSlug,  Types.VARCHAR)
                .addValue("projectToken", isBlank(projectToken) ? null : projectToken, Types.VARCHAR)
                .addValue("windowDays",   windowDays)
                .addValue("minRuns",      minRuns)
                .addValue("offset",       offset)
                .addValue("limit",        sizePlusOne);


        return jdbc.query(sql, params, (rs, rowNum) -> mapItem(rs));
    }

    private static TopFlakyItem mapItem(ResultSet rs) throws SQLException {
        TopFlakyItem i = new TopFlakyItem();

        java.util.UUID caseUuid = rs.getObject("test_case_id", java.util.UUID.class);
        i.testCaseId = (caseUuid != null) ? caseUuid.toString() : null;

        i.className = rs.getString("class_name");
        i.testName  = rs.getString("test_name");
        i.runs      = rs.getInt("runs");
        i.fails     = rs.getInt("fails");
        i.passes    = rs.getInt("passes");
        i.failRate  = rs.getDouble("fail_rate");
        i.flips     = rs.getInt("flips");

        java.sql.Timestamp ts = rs.getTimestamp("last_seen_at");
        i.lastSeenAt = (ts != null) ? ts.toInstant() : null;
        return i;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
