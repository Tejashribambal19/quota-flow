package com.tejashri.quota.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejashri.quota.dto.ConsumeUsageRequest;
import com.tejashri.quota.dto.UsageDecisionResponse;
import com.tejashri.quota.dto.UsageEventResponse;
import com.tejashri.quota.service.UsageMeteringService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageMeteringService usageMeteringService;

    @GetMapping("/{tenantId}/events")
    public ResponseEntity<List<UsageEventResponse>> recentEvents(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(usageMeteringService.getRecentEvents(tenantId));
    }

    @PostMapping("/{tenantId}/consume")
    public ResponseEntity<UsageDecisionResponse> consume(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ConsumeUsageRequest request
    ) {
        UsageDecisionResponse response =
                usageMeteringService.consume(tenantId, request);

        HttpStatus status = response.allowed()
                ? HttpStatus.OK
                : HttpStatus.TOO_MANY_REQUESTS;

        return ResponseEntity.status(status).body(response);
    }
}
