package com.fto.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {
    private final JdbcTemplate jdbc;

    public DebugController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/top-flaky")
    public List<Map<String,Object>> topFlaky(@RequestParam(defaultValue = "20") int limit) {
        return jdbc.query("""
            SELECT tc.suite, tc.name, tc.file, f.flakiness_score
            FROM flakes f
            JOIN test_cases tc ON tc.id = f.case_id
            ORDER BY f.flakiness_score DESC
            LIMIT ?
            """, (rs, i) -> Map.of(
                "suite", rs.getString("suite"),
                "name", rs.getString("name"),
                "file", rs.getString("file"),
                "score", rs.getBigDecimal("flakiness_score")
        ), limit);
    }
}
