package com.fto.controller;

import com.fto.service.IngestService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;

@RestController
@RequestMapping(value = "/ingest", produces = MediaType.APPLICATION_JSON_VALUE)
public class IngestController {

    private final IngestService service;

    public IngestController(IngestService service) {
        this.service = service;
    }

    // 1) MULTIPART FORM-DATA
    @PostMapping(path = "/junit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ingestMultipart(
            @RequestHeader(name = "X-Project-Token", required = false) String token,
            @RequestHeader(name = "X-CI-Job", required = false) String ciJobId,
            @RequestHeader(name = "X-Commit", required = false) String commitSha,
            @RequestHeader(name = "X-Branch", required = false) String branch,
            @RequestHeader(name = "X-Artifacts-Url", required = false) String artifactsUrl,
            @RequestPart("file") MultipartFile file
    ) {
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

    // 2) RAW XML (application/xml | text/xml)
    @PostMapping(path = "/junit",
            consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public ResponseEntity<?> ingestRawXml(
            @RequestHeader(name = "X-Project-Token", required = false) String token,
            @RequestHeader(name = "X-CI-Job", required = false) String ciJobId,
            @RequestHeader(name = "X-Commit", required = false) String commitSha,
            @RequestHeader(name = "X-Branch", required = false) String branch,
            @RequestHeader(name = "X-Artifacts-Url", required = false) String artifactsUrl,
            @RequestBody byte[] body
    ) {
        try {
            if (!StringUtils.hasText(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing X-Project-Token"));
            }
            if (body == null || body.length == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty body"));
            }

            Map<String, Object> result = service.ingest(
                    token,
                    new ByteArrayInputStream(body),
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
