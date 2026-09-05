package com.tejashri.quota.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ApiClient {

    private static final String BASE_URL
            = System.getenv().getOrDefault(
                    "QUOTA_API_URL",
                    "http://localhost:8080/api"
            );

    private final HttpClient httpClient
            = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper
            = new ObjectMapper();

    public AuthResponse login(
            String email,
            String password
    ) throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", email);
        body.put("password", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)
                ))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        ensureSuccess(response);

        return objectMapper.readValue(
                response.body(),
                AuthResponse.class
        );
    }

    public UsageSummaryResponse getUsageSummary(
            UUID tenantId,
            String token
    ) throws IOException, InterruptedException {

        String responseBody = authenticatedGet(
                "/reports/tenants/"
                + tenantId
                + "/usage",
                token
        );

        return objectMapper.readValue(
                responseBody,
                UsageSummaryResponse.class
        );
    }

    public BillingReportResponse getBillingReport(
            UUID tenantId,
            String token
    ) throws IOException, InterruptedException {

        String responseBody = authenticatedGet(
                "/reports/tenants/"
                + tenantId
                + "/billing",
                token
        );

        return objectMapper.readValue(
                responseBody,
                BillingReportResponse.class
        );
    }

    public List<PlanResponse> getPlans(
            String token
    ) throws IOException, InterruptedException {

        String responseBody = authenticatedGet(
                "/plans",
                token
        );

        return objectMapper.readValue(
                responseBody,
                new TypeReference<List<PlanResponse>>() {
        }
        );
    }

    public List<TenantResponse> getTenants(
            String token
    ) throws IOException, InterruptedException {

        String responseBody = authenticatedGet(
                "/tenants",
                token
        );

        return objectMapper.readValue(
                responseBody,
                new TypeReference<List<TenantResponse>>() {
        }
        );
    }

    public AuditVerificationResponse verifyAuditChain(
            String token
    ) throws IOException, InterruptedException {

        String responseBody = authenticatedGet(
                "/audit/verify",
                token
        );

        return objectMapper.readValue(
                responseBody,
                AuditVerificationResponse.class
        );
    }

    public UsageDecisionResponse consumeUsage(
            UUID tenantId,
            String resourceType,
            long quantity,
            String token
    ) throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("resourceType", resourceType);
        body.put("quantity", quantity);
        body.put(
                "requestId",
                "desktop-simulator-" + UUID.randomUUID()
        );

        String responseBody = authenticatedPost(
                "/usage/" + tenantId + "/consume",
                token,
                body
        );

        return objectMapper.readValue(
                responseBody,
                UsageDecisionResponse.class
        );
    }

    public TenantResponse createTenant(
            String name,
            String slug,
            UUID planId,
            int billingCycleDay,
            String token
    ) throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", name);
        body.put("slug", slug);
        body.put("planId", planId.toString());
        body.put("billingCycleDay", billingCycleDay);

        String responseBody = authenticatedPost(
                "/tenants",
                token,
                body
        );

        return objectMapper.readValue(
                responseBody,
                TenantResponse.class
        );
    }

    public PlanResponse createPlan(
            String name,
            String description,
            BigDecimal monthlyPrice,
            long apiLimit,
            long storageLimit,
            long computeLimit,
            long jobLimit,
            String token
    ) throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", name);
        body.put("description", description);
        body.put("monthlyPrice", monthlyPrice);

        var quotas = body.putArray("quotas");

        addQuota(
                quotas.addObject(),
                "API_REQUEST",
                apiLimit
        );

        addQuota(
                quotas.addObject(),
                "STORAGE_MB",
                storageLimit
        );

        addQuota(
                quotas.addObject(),
                "COMPUTE_SECOND",
                computeLimit
        );

        addQuota(
                quotas.addObject(),
                "BACKGROUND_JOB",
                jobLimit
        );

        String responseBody = authenticatedPost(
                "/plans",
                token,
                body
        );

        return objectMapper.readValue(
                responseBody,
                PlanResponse.class
        );
    }

    public Path downloadInvoice(
            UUID tenantId,
            String token,
            Path destination
    ) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        BASE_URL
                        + "/reports/tenants/"
                        + tenantId
                        + "/invoice.pdf"
                ))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                    "Invoice download failed with status "
                    + response.statusCode()
            );
        }

        Files.write(destination, response.body());

        return destination;
    }

    private void addQuota(
            ObjectNode quota,
            String resourceType,
            long hardLimit
    ) {
        quota.put("resourceType", resourceType);
        quota.put("hardLimit", hardLimit);
        quota.put("warningPercentage", 80);
        quota.put("criticalPercentage", 90);
    }

    private String authenticatedPost(
            String path,
            String token,
            ObjectNode body
    ) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)
                ))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        ensureSuccess(response);

        return response.body();
    }

    private String authenticatedGet(
            String path,
            String token
    ) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        ensureSuccess(response);

        return response.body();
    }

    private void ensureSuccess(
            HttpResponse<String> response
    ) {
        if (response.statusCode() >= 200
                && response.statusCode() < 300) {
            return;
        }

        String message
                = "Request failed with status "
                + response.statusCode();

        String responseBody = response.body();

        if (responseBody != null
                && !responseBody.isBlank()) {

            try {
                JsonNode errorJson
                        = objectMapper.readTree(responseBody);

                if (errorJson.hasNonNull("message")) {
                    message = errorJson
                            .get("message")
                            .asText();
                }
            } catch (Exception ignored) {
                message = responseBody;
            }
        }

        throw new IllegalStateException(message);
    }

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresIn,
            UUID userId,
            String fullName,
            String email,
            String role,
            UUID tenantId
            ) {

    }

    public record UsageSummaryResponse(
            UUID tenantId,
            String tenantName,
            String planName,
            String billingMonth,
            BigDecimal monthlyPrice,
            List<ResourceUsage> resources
            ) {

    }

    public record ResourceUsage(
            String resourceType,
            long used,
            long limit,
            double percentage,
            String level,
            BigDecimal allocatedCost,
            BigDecimal consumedCost
            ) {

    }

    public record BillingReportResponse(
            String invoiceNumber,
            UUID tenantId,
            String tenantName,
            String planName,
            String billingMonth,
            BigDecimal baseAmount,
            BigDecimal utilizedValue,
            BigDecimal totalPayable,
            String status,
            List<ResourceUsage> usageDetails
            ) {

    }

    public record PlanResponse(
            UUID id,
            String name,
            String description,
            BigDecimal monthlyPrice,
            boolean active,
            List<QuotaResponse> quotas
            ) {

    }

    public record QuotaResponse(
            UUID id,
            String resourceType,
            long hardLimit,
            int warningPercentage,
            int criticalPercentage
            ) {

    }

    public record TenantResponse(
            UUID id,
            String name,
            String slug,
            String status,
            UUID planId,
            String planName,
            int billingCycleDay
            ) {

    }

    public record AuditVerificationResponse(
            boolean valid,
            String message
            ) {

    }

    public record UsageDecisionResponse(
            UUID tenantId,
            String resourceType,
            boolean allowed,
            long requestedQuantity,
            long used,
            long limit,
            double percentage,
            String level,
            String message
            ) {

    }
}
