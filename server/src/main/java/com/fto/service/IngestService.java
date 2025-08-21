package com.fto.service;

import com.fto.api.ingest.StatusNormalizer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class IngestService {

    private final JdbcTemplate jdbc;
    private final JunitXmlParser parser = new JunitXmlParser();

    public IngestService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> ingest(String projectToken,
                                      InputStream junitXml,
                                      String ciJobId,
                                      String commitSha,
                                      String branch,
                                      String artifactsUrl) throws Exception {

        UUID projectId = findProjectIdByToken(projectToken);
        if (projectId == null) throw new IllegalArgumentException("Invalid project token");

        byte[] bytes = junitXml.readAllBytes();
        String importHash = sha256Hex(bytes);

        // Idempotent: aynı import_hash varsa mevcut run'ı döndür
        UUID existingRun = findRunByImportHash(projectId, importHash);
        if (existingRun != null) {
            return Map.of(
                    "duplicate", true,
                    "runId", existingRun.toString()
            );
        }

        JunitXmlParser.ParsedReport report = parser.parse(new java.io.ByteArrayInputStream(bytes));
        Instant startedAt = report.startedAt != null ? report.startedAt : Instant.now();

        UUID runId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO test_runs(id, project_id, ci_job_id, commit_sha, branch, started_at, duration_ms, status, env_hash, import_hash)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """,
                runId, projectId, ciJobId, commitSha, branch, Timestamp.from(startedAt), null, "done", null, importHash
        );

        int inserted = 0, failed = 0, skipped = 0, passed = 0, errored = 0;
        Set<UUID> touchedCases = new HashSet<>();

        for (JunitXmlParser.ParsedCase pc : report.cases) {
            UUID caseId = upsertTestCase(projectId, pc.suite, pc.name, pc.file);
            touchedCases.add(caseId);
            String rawStatus = pc.status;
            String status = StatusNormalizer.normalize(rawStatus);
            String errHash = (status.equals("failed") || status.equals("error")) && pc.failureMessage != null
                    ? sha1Hex(pc.failureMessage)
                    : null;

            jdbc.update("""
                INSERT INTO test_results(id, run_id, case_id, status, duration_ms, error_hash, retries, artifacts_url)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, 0, ?)
                """,
                    runId, caseId, status, (int)pc.durationMs, errHash, artifactsUrl
            );

            inserted++;
            switch (status) {
                case "FAILED"  -> { failed++; if (rawStatus != null && rawStatus.equalsIgnoreCase("error")) errored++; }
                case "SKIPPED" -> skipped++;
                case "PASSED"  -> passed++;
            }
        }

        // Flakiness skorlarını güncelle (dokunulan case'ler için)
        for (UUID caseId : touchedCases) {
            updateFlakinessScore(caseId);
        }

        return Map.of(
                "duplicate", false,
                "runId", runId.toString(),
                "cases", inserted,
                "passed", passed,
                "failed", failed,
                "skipped", skipped,
                "error", errored
        );
    }

    private UUID findProjectIdByToken(String token) {
        List<UUID> list = jdbc.query("SELECT id FROM projects WHERE token = ?", (rs, i) -> (UUID) rs.getObject(1), token);
        return list.isEmpty() ? null : list.get(0);
    }

    private UUID findRunByImportHash(UUID projectId, String hash) {
        List<UUID> list = jdbc.query("SELECT id FROM test_runs WHERE project_id = ? AND import_hash = ?",
                (rs, i) -> (UUID) rs.getObject(1), projectId, hash);
        return list.isEmpty() ? null : list.get(0);
    }

    private UUID upsertTestCase(UUID projectId, String suite, String name, String file) {
        List<UUID> found = jdbc.query("""
            SELECT id FROM test_cases WHERE project_id=? AND COALESCE(suite,'')=? AND name=? AND COALESCE(file,'')=COALESCE(?, '')
            """, (rs, i) -> (UUID) rs.getObject(1),
                projectId, nz(suite), name, file
        );
        if (!found.isEmpty()) return found.get(0);

        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO test_cases(id, project_id, suite, name, file)
            VALUES (?,?,?,?,?)
            """, id, projectId, nz(suite), name, file);
        return id;
    }

    private void updateFlakinessScore(UUID caseId) {
        // Son 20 sonucu çek
        List<String> statuses = jdbc.query("""
            SELECT status FROM test_results WHERE case_id = ? ORDER BY id DESC LIMIT 20
            """, (rs, i) -> rs.getString(1), caseId);

        long pass = statuses.stream().filter(s -> s.equals("passed")).count();
        long fail = statuses.stream().filter(s -> s.equals("failed") || s.equals("error")).count();
        if (pass > 0 && fail > 0) {
            double score = (double) fail / (double) (pass + fail); // 0..1
            // Upsert flakes
            int updated = jdbc.update("""
                UPDATE flakes SET last_seen = now(), flakiness_score = ?
                WHERE case_id = ?
                """, score, caseId);
            if (updated == 0) {
                jdbc.update("""
                    INSERT INTO flakes(id, case_id, first_seen, last_seen, flakiness_score)
                    VALUES (gen_random_uuid(), ?, now(), now(), ?)
                    """, caseId, score);
            }
        } else {
            // Flaky değil -> skor 0 yapılabilir
        }
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return hex(md.digest(data));
    }

    private static String sha1Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return hex(md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
