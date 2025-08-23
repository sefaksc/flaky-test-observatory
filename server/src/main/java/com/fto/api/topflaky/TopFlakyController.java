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


    public TopFlakyController(TopFlakyService service) { this.service = service; }


    @GetMapping("/top-flaky")
    public ResponseEntity<TopFlakyResponse> topFlaky(
            @RequestParam(name = "project", required = false, defaultValue = "demo") String project,
            @RequestHeader(name = "X-Project-Token", required = false) String token,
            @RequestParam(name = "windowDays", required = false, defaultValue = "14") int windowDays,
            @RequestParam(name = "minRuns", required = false, defaultValue = "5") int minRuns,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestParam(name = "branch", required = false) String branch,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "sort", required = false, defaultValue = "score") String sort,
            @RequestParam(name = "dir", required = false, defaultValue = "desc") String dir
    ) {
        final int normalizedPage = Math.max(1, page);
        final int normalizedSize = Math.min(Math.max(1, size), 200);
        final int normalizedWindow = Math.min(Math.max(1, windowDays), 365);
        final int normalizedMinRuns = Math.max(1, minRuns);


        final String tokenTrim = StringUtils.hasText(token) ? token.trim() : null;
        final String projectTrim = (tokenTrim != null) ? null : (StringUtils.hasText(project) ? project.trim() : null);
        final String branchTrim = StringUtils.hasText(branch) ? branch.trim() : null;
        final String qTrim = StringUtils.hasText(q) ? q.trim() : null;


        final int limit = normalizedSize + 1;
        final int offset = (normalizedPage - 1) * normalizedSize;


        List<TopFlakyItem> rows = service.query(
                projectTrim, tokenTrim,
                normalizedWindow, normalizedMinRuns,
                branchTrim, qTrim, sort, dir,
                limit, offset
        );


        boolean hasNext = rows.size() > normalizedSize;
        if (hasNext) rows = rows.subList(0, normalizedSize);


        TopFlakyResponse resp = new TopFlakyResponse();
        resp.items = rows;
        resp.page = normalizedPage;
        resp.size = normalizedSize;
        resp.hasNext = hasNext;
        resp.windowDays = normalizedWindow;
        resp.minRuns = normalizedMinRuns;
        resp.project = (projectTrim != null ? projectTrim : "token");
        return ResponseEntity.ok(resp);
    }
}