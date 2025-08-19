package com.fto.controller;

import com.fto.service.IngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class IngestController {

    private final IngestService service;

    public IngestController(IngestService service) {
        this.service = service;
    }

    @PostMapping(path = "/ingest/junit", consumes = {"multipart/form-data"})
    public ResponseEntity<?> ingest(
            @RequestHeader(name = "X-Project-Token", required = false) String token,
            @RequestHeader(name = "X-CI-Job", required = false) String ciJobId,
            @RequestHeader(name = "X-Commit", required = false) String commitSha,
            @RequestHeader(name = "X-Branch", required = false) String branch,
            @RequestHeader(name = "X-Artifacts-Url", required = false) String artifactsUrl,
            @RequestPart("file") MultipartFile file) {
        try {
            if (!StringUtils.hasText(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing X-Project-Token"));
            }
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
            }

            Map<String, Object> result = service.ingest(
                    token,
                    file.getInputStream(),
                    ciJobId, commitSha, branch, artifactsUrl
            );
            return ResponseEntity.accepted().body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
