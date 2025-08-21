package com.fto.api.topflaky;

import com.fto.api.topflaky.dto.TopFlakyItem;
import com.fto.api.topflaky.dto.TopFlakyResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class TopFlakyController {
    private final TopFlakyService service;

    public TopFlakyController(TopFlakyService service) {
        this.service = service;
    }

    @GetMapping("/top-flaky")
    public ResponseEntity<TopFlakyResponse> topFlaky(
            @RequestParam(name = "project", required = false, defaultValue = "demo") String project,
            @RequestHeader(name = "X-Project-Token", required = false) String token,
            @RequestParam(name = "windowDays", required = false, defaultValue = "14") int windowDays,
            @RequestParam(name = "minRuns", required = false, defaultValue = "5") int minRuns,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        // ---- 1) Parametre normalization / guardrails ----
        final int normalizedPage      = Math.max(1, page);
        final int normalizedSize      = Math.min(Math.max(1, size), 200);   // üst sınır: 200
        final int normalizedWindow    = Math.min(Math.max(1, windowDays), 365); // üst sınır: 365 gün
        final int normalizedMinRuns   = Math.max(1, minRuns);

        // Boşlukları kırp
        final String projectTrimmed = StringUtils.hasText(project) ? project.trim() : null;
        final String tokenTrimmed   = StringUtils.hasText(token)   ? token.trim()   : null;

        // ---- 2) Token varsa onu tercih et (project=null geç) ----
        final String projectForQuery = (tokenTrimmed != null) ? null : projectTrimmed;
        final String tokenForQuery   = tokenTrimmed;

        final int limit  = normalizedSize + 1; // hasNext için +1
        final int offset = (normalizedPage - 1) * normalizedSize;

        // ---- 3) Sorgu ----
        List<TopFlakyItem> rows = service.query(
                projectForQuery,
                tokenForQuery,
                normalizedWindow,
                normalizedMinRuns,
                limit,
                offset
        );

        boolean hasNext = rows.size() > normalizedSize;
        if (hasNext) {
            rows = rows.subList(0, normalizedSize);
        }

        // ---- 4) Yanıt ----
        TopFlakyResponse resp = new TopFlakyResponse();
        resp.items      = rows;
        resp.page       = normalizedPage;
        resp.size       = normalizedSize;
        resp.hasNext    = hasNext;
        resp.windowDays = normalizedWindow;
        resp.minRuns    = normalizedMinRuns;
        resp.project    = (projectForQuery != null ? projectForQuery : "token");

        return ResponseEntity.ok(resp);
    }
}
