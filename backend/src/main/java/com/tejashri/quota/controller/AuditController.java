package com.tejashri.quota.controller;

import com.tejashri.quota.domain.AuditLog;
import com.tejashri.quota.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getLogs() {
        return ResponseEntity.ok(auditService.getLogs());
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        boolean valid = auditService.verifyChain();

        return ResponseEntity.ok(
                Map.of(
                        "valid", valid,
                        "message", valid
                                ? "Audit chain is valid"
                                : "Audit chain has been tampered with"
                )
        );
    }
}