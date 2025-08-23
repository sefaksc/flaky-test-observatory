package com.fto.api.topflaky;


import com.fto.api.topflaky.dto.TopFlakyItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Service
public class TopFlakyService {
    private final NamedParameterJdbcTemplate jdbc;


    public TopFlakyService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TopFlakyItem> query(String projectSlug, String projectToken,
                                    int windowDays, int minRuns,
                                    String branch, String q,
                                    String sort, String dir,
                                    int sizePlusOne, int offset) {

        Map<String, String> allowed = Map.of(
                "score", "score",
                "flips", "flips",
                "failRate", "fail_rate",
                "runs", "runs",
                "lastSeen", "last_seen_at"
        );

        String orderCol = allowed.getOrDefault(sort == null ? "score" : sort, "score");
        String orderDir = "asc".equalsIgnoreCase(dir) ? "ASC" : "DESC";

        // Smoothing sabitleri (gerekirse ileride config’e alınır)
        double kf = 5.0;   // flip_rate shrinkage
        double kc = 10.0;  // cv shrinkage
        double halfLife = Math.max(1, windowDays / 2.0); // decay yarı ömrü

        String sql = """
                WITH windowed AS (
                  SELECT
                    tr.id           AS run_id,
                    tr.started_at   AS run_started_at,
                    tr.branch,
                    r.case_id       AS test_case_id,
                    tc.suite        AS class_name,
                    tc.name         AS test_name,
                    r.status,
                    r.duration_ms
                  FROM test_runs tr
                  JOIN projects p     ON p.id = tr.project_id
                  JOIN test_results r ON r.run_id = tr.id
                  JOIN test_cases tc  ON tc.id = r.case_id
                  WHERE (
                    (:projectSlug   IS NOT NULL AND p.name  = :projectSlug)
                    OR (:projectToken IS NOT NULL AND p.token = :projectToken)
                  )
                  AND tr.started_at >= NOW() - (:windowDays || ' days')::interval
                  AND (:branch IS NULL OR tr.branch = :branch)
                  AND (:q IS NULL OR tc.suite ILIKE ('%' || :q || '%') OR tc.name ILIKE ('%' || :q || '%'))
                ),
                ordered AS (
                  SELECT
                    test_case_id,
                    class_name,
                    test_name,
                    run_started_at,
                    status,
                    duration_ms,
                    LAG(status) OVER (PARTITION BY test_case_id ORDER BY run_started_at) AS prev_status
                  FROM windowed
                ),
                agg AS (
                  SELECT
                    test_case_id,
                    MAX(class_name) AS class_name,
                    MAX(test_name)  AS test_name,
                    COUNT(*) AS runs,
                    COUNT(*) FILTER (WHERE status = 'FAILED') AS fails,
                    COUNT(*) FILTER (WHERE status = 'PASSED') AS passes,
                    COUNT(*) FILTER (WHERE prev_status IS NOT NULL AND status <> prev_status) AS flips,
                    AVG(duration_ms)::double precision AS avg_ms,
                    COALESCE(stddev_samp(duration_ms), 0)::double precision AS stddev_ms,
                    MAX(run_started_at) AS last_seen_at
                  FROM ordered
                  GROUP BY test_case_id
                ),
                totals AS (
                  SELECT
                    /* global ortalamalar (shrinkage merkezleri) */
                    AVG(CASE WHEN runs>0 THEN fails::double precision / runs ELSE 0 END)                                       AS mu_fail,
                    AVG(CASE WHEN runs>1 THEN flips::double precision / (runs - 1) ELSE 0 END)                                  AS mu_flip,
                    AVG(CASE WHEN avg_ms>0 THEN LEAST(GREATEST(stddev_ms / avg_ms, 0), 1) ELSE 0 END)                           AS mu_cv
                  FROM agg
                ),
                scored AS (
                  SELECT
                    a.test_case_id,
                    a.class_name,
                    a.test_name,
                    a.runs,
                    a.fails,
                    a.passes,
                    a.flips,
                    a.last_seen_at,

                    /* ham oranlar (şeffaflık için API’de gösterilecek) */
                    CASE WHEN a.runs = 0 THEN 0 ELSE (a.fails::double precision / a.runs::double precision) END                 AS fail_rate_raw,
                    CASE WHEN a.runs <= 1 THEN 0 ELSE (a.flips::double precision / (a.runs - 1)) END                            AS flip_rate_raw,
                    CASE WHEN a.avg_ms <= 0 THEN 0 ELSE LEAST(GREATEST(a.stddev_ms / NULLIF(a.avg_ms,0), 0), 1) END             AS cv_raw,

                    /* Jeffreys smoothing (α=β=0.5) -> fail_rate */
                    (a.fails + 0.5)::double precision / (a.runs + 1.0)::double precision                                        AS fail_rate_s,

                    /* flip_rate shrinkage (kf gözlemle global ortalamaya çek) */
                    CASE WHEN a.runs <= 1 THEN 0 ELSE
                      ( (a.flips::double precision/(a.runs - 1)) * ((a.runs - 1)::double precision / ((a.runs - 1) + :kf))
                        + (SELECT mu_flip FROM totals) * (:kf / (((a.runs - 1) + :kf)::double precision)) )
                    END                                                                                                          AS flip_rate_s,

                    /* cv shrinkage (kc gözlemle global ortalamaya çek) */
                    (
                      (CASE WHEN a.avg_ms>0 THEN LEAST(GREATEST(a.stddev_ms / a.avg_ms, 0), 1) ELSE 0 END)
                        * (a.runs::double precision / (a.runs + :kc))
                      + (SELECT mu_cv FROM totals) * (:kc / (a.runs + :kc))
                    )                                                                                                            AS cv_s,

                    /* tazelik ve decay */
                    EXTRACT(EPOCH FROM (NOW() - a.last_seen_at)) / 86400.0                                                       AS days_since_last,
                    (1.0 / (1.0 + ((EXTRACT(EPOCH FROM (NOW() - a.last_seen_at)) / 86400.0) / GREATEST(:halfLifeDays::double precision, 1)))) AS decay
                  FROM agg a
                )
                SELECT
                  test_case_id,
                  class_name,
                  test_name,
                  runs,
                  fails,
                  passes,
                  /* API çıktısında gösterilecek fail_rate: ham oran (değiştirmedik) */
                  fail_rate_raw AS fail_rate,
                  flips,
                  last_seen_at,
                  /* skor v0: smoothed metriklerle */
                  ((0.6*fail_rate_s + 0.3*flip_rate_s + 0.1*cv_s) * decay) AS score
                FROM scored
                WHERE runs >= :minRuns AND fails > 0 AND passes > 0
                ORDER BY
                """ + orderCol + " " + orderDir + """
                , last_seen_at DESC, runs DESC
                OFFSET :offset LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("projectSlug",  (projectSlug == null || projectSlug.isBlank()) ? null : projectSlug, Types.VARCHAR)
                .addValue("projectToken", (projectToken == null || projectToken.isBlank()) ? null : projectToken, Types.VARCHAR)
                .addValue("windowDays",   windowDays)
                .addValue("minRuns",      minRuns)
                .addValue("branch",       (branch == null || branch.isBlank()) ? null : branch, Types.VARCHAR)
                .addValue("q",            (q == null || q.isBlank()) ? null : q, Types.VARCHAR)
                .addValue("halfLifeDays", halfLife)
                .addValue("kf",           kf)
                .addValue("kc",           kc)
                .addValue("offset",       offset)
                .addValue("limit",        sizePlusOne);


        return jdbc.query(sql, params, (rs, rowNum) -> mapItem(rs));
    }

    private static TopFlakyItem mapItem(ResultSet rs) throws SQLException {
        TopFlakyItem i = new TopFlakyItem();
        java.util.UUID caseUuid = rs.getObject("test_case_id", java.util.UUID.class);
        i.testCaseId = (caseUuid != null) ? caseUuid.toString() : null;
        i.className = rs.getString("class_name");
        i.testName = rs.getString("test_name");
        i.runs = rs.getInt("runs");
        i.fails = rs.getInt("fails");
        i.passes = rs.getInt("passes");
        i.failRate = rs.getDouble("fail_rate");
        i.flips = rs.getInt("flips");
        java.sql.Timestamp ts = rs.getTimestamp("last_seen_at");
        i.lastSeenAt = (ts != null) ? ts.toInstant() : null;
        i.score = rs.getDouble("score");
        return i;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
