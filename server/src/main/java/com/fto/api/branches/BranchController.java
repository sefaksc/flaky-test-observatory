package com.fto.api.branches;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BranchController {
    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    @GetMapping("/api/branches")
    public ResponseEntity<List<BranchItem>> branches(
            @RequestParam(name = "project", required = false) String project,
            @RequestHeader(name = "X-Project-Token", required = false) String token,
            @RequestParam(name = "windowDays", required = false) Integer windowDays,
            @RequestParam(name = "limit", required = false, defaultValue = "200") Integer limit
    ) {
        return ResponseEntity.ok(service.list(project, token, windowDays, limit));
    }
}
