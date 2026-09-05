package com.tejashri.quota.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejashri.quota.dto.CreatePlanRequest;
import com.tejashri.quota.dto.PlanResponse;
import com.tejashri.quota.service.SubscriptionPlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(
            @Valid @RequestBody CreatePlanRequest request
       ) {
        PlanResponse response = planService.createPlan(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }
}