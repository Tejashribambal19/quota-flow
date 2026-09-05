package com.tejashri.quota.client;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class QuotaDesktopApplication extends Application {

    private final ApiClient apiClient = new ApiClient();
    private final Map<String, Node> navigationTargets = new HashMap<>();
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        stage.setTitle("Quota Platform");
        stage.setMinWidth(950);
        stage.setMinHeight(650);

        showLogin();
        stage.show();
    }

    private VBox createAlertPanel(
            List<ApiClient.ResourceUsage> resources
    ) {
        VBox alerts = new VBox(10);
        alerts.getStyleClass().add("alert-panel");

        boolean alertFound = false;

        for (ApiClient.ResourceUsage resource : resources) {
            String level = resource.level();

            if ("WARNING".equals(level)
                    || "CRITICAL".equals(level)
                    || "BLOCKED".equals(level)) {

                Label alert = new Label(
                        createAlertMessage(resource)
                );

                alert.setWrapText(true);
                alert.setMaxWidth(Double.MAX_VALUE);
                alert.getStyleClass().add("quota-alert");
                alert.getStyleClass().add(
                        "alert-" + level.toLowerCase()
                );

                alerts.getChildren().add(alert);
                alertFound = true;
            }
        }

        if (!alertFound) {
            Label normal = new Label(
                    "✓ All resources are operating within their quota limits."
            );

            normal.setMaxWidth(Double.MAX_VALUE);
            normal.getStyleClass().addAll(
                    "quota-alert",
                    "alert-normal"
            );

            alerts.getChildren().add(normal);
        }

        return alerts;
    }

    private String createAlertMessage(
            ApiClient.ResourceUsage resource
    ) {
        String resourceName
                = resource.resourceType().replace("_", " ");

        return switch (resource.level()) {
            case "WARNING" ->
                "⚠ " + resourceName
                + " usage has reached "
                + resource.percentage()
                + "%. Monitor usage carefully.";

            case "CRITICAL" ->
                "⚠ CRITICAL: " + resourceName
                + " usage is "
                + resource.percentage()
                + "%. Only "
                + Math.max(0, resource.limit() - resource.used())
                + " units remain.";

            case "BLOCKED" ->
                "⛔ " + resourceName
                + " has reached its quota limit. "
                + "New usage requests will be blocked.";

            default ->
                resourceName + " usage is normal.";
        };
    }

    private BarChart<String, Number> createUsageChart(
            List<ApiClient.ResourceUsage> resources
    ) {
        CategoryAxis resourceAxis = new CategoryAxis();

        NumberAxis percentageAxis = new NumberAxis(
                0,
                100,
                20
        );

        percentageAxis.setLabel("Usage percentage");

        BarChart<String, Number> chart
                = new BarChart<>(resourceAxis, percentageAxis);

        chart.setTitle("Monthly resource utilization");
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> series
                = new XYChart.Series<>();

        for (ApiClient.ResourceUsage resource : resources) {
            series.getData().add(
                    new XYChart.Data<>(
                            resource.resourceType().replace("_", " "),
                            resource.percentage()
                    )
            );
        }

        chart.getData().add(series);

        return chart;
    }

    private VBox createUsageSimulator(
            ApiClient.AuthResponse authentication,
            VBox content
    ) {
        Label title = new Label("Live usage simulator");
        title.getStyleClass().add("section-title");

        Label description = new Label(
                "Simulate customer activity and update quota usage in real time."
        );
        description.getStyleClass().add("muted");

        ComboBox<String> resourceBox = new ComboBox<>();
        resourceBox.getItems().addAll(
                "API_REQUEST",
                "STORAGE_MB",
                "COMPUTE_SECOND",
                "BACKGROUND_JOB"
        );
        resourceBox.setValue("API_REQUEST");
        resourceBox.setPrefWidth(230);

        TextField quantityField = new TextField("10");
        quantityField.setPromptText("Quantity");
        quantityField.setPrefWidth(160);

        Button simulateButton = new Button("Simulate usage");
        simulateButton.getStyleClass().add("primary-button");

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);

        simulateButton.setOnAction(event -> {
            String resourceType = resourceBox.getValue();
            long quantity;

            try {
                quantity = Long.parseLong(quantityField.getText().trim());

                if (quantity <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                resultLabel.getStyleClass().setAll("error-label");
                resultLabel.setText("Enter a positive whole-number quantity.");
                return;
            }

            simulateButton.setDisable(true);
            simulateButton.setText("Processing...");
            resultLabel.setText("");

            long requestedQuantity = quantity;

            CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return apiClient.consumeUsage(
                                    authentication.tenantId(),
                                    resourceType,
                                    requestedQuantity,
                                    authentication.token()
                            );
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .whenComplete((decision, error) ->
                            Platform.runLater(() -> {
                                simulateButton.setDisable(false);
                                simulateButton.setText("Simulate usage");

                                if (error != null) {
                                    Throwable cause = error;

                                    while (cause.getCause() != null) {
                                        cause = cause.getCause();
                                    }

                                    resultLabel.getStyleClass().setAll("error-label");
                                    resultLabel.setText(
                                            "Simulation failed: " + cause.getMessage()
                                    );
                                    return;
                                }

                                resultLabel.getStyleClass().setAll("success-label");
                                resultLabel.setText(
                                        decision.message()
                                                + " — Used "
                                                + decision.used()
                                                + " / "
                                                + decision.limit()
                                );

                                loadTenantDashboard(authentication, content);
                            })
                    );
        });

        HBox controls = new HBox(
                14,
                resourceBox,
                quantityField,
                simulateButton
        );
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox simulatorCard = new VBox(
                14,
                description,
                controls,
                resultLabel
        );
        simulatorCard.getStyleClass().add("simulator-card");

        return new VBox(12, title, simulatorCard);
    }

    private void showLogin() {
        Label brand = new Label("QUOTA FLOW");
        brand.getStyleClass().add("brand");

        Label title = new Label("Welcome back");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label(
                "Sign in to manage tenants or monitor resource usage"
        );
        subtitle.getStyleClass().add("muted");

        Label emailLabel = new Label("Email");

        TextField emailField = new TextField();
        emailField.setPromptText("Email address");
        emailField.setText("admin@abclogistics.com");

        Label passwordLabel = new Label("Password");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);

        Button loginButton = new Button("Sign in");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(
                14,
                brand,
                title,
                subtitle,
                emailLabel,
                emailField,
                passwordLabel,
                passwordField,
                errorLabel,
                loginButton
        );

        form.getStyleClass().add("login-card");
        form.setMaxWidth(420);

        StackPane root = new StackPane(form);
        root.getStyleClass().add("login-background");
        root.setPadding(new Insets(40));

        loginButton.setOnAction(event -> login(
                emailField,
                passwordField,
                errorLabel,
                loginButton
        ));

        passwordField.setOnAction(event -> loginButton.fire());

        Scene scene = new Scene(root, 1050, 700);

        applyStyles(scene);

        stage.setScene(scene);
    }

    private void login(
            TextField emailField,
            PasswordField passwordField,
            Label errorLabel,
            Button loginButton
    ) {
        errorLabel.setText("");

        if (emailField.getText().isBlank()
                || passwordField.getText().isBlank()) {
            errorLabel.setText("Email and password are required.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Signing in...");

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return apiClient.login(
                                emailField.getText().trim(),
                                passwordField.getText()
                        );
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .whenComplete((authentication, error)
                        -> Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Sign in");

                    if (error != null) {
                        errorLabel.setText(
                                "Login failed. Check your credentials and backend."
                        );
                        return;
                    }

                    if ("PLATFORM_ADMIN".equals(authentication.role())) {
                        showPlatformAdminDashboard(authentication);
                        return;
                    }

                    if ("TENANT_ADMIN".equals(authentication.role())) {
                        if (authentication.tenantId() == null) {
                            errorLabel.setText(
                                    "This tenant administrator is not assigned to a tenant."
                            );
                            return;
                        }

                        showTenantDashboard(authentication);
                        return;
                    }

                    errorLabel.setText("Unsupported user role.");
                })
                );
    }

    private HBox createHeader(
            ApiClient.AuthResponse authentication
    ) {
        Label logo = new Label("QUOTA FLOW");
        logo.getStyleClass().add("brand-small");

        Label user = new Label(authentication.fullName());
        user.getStyleClass().add("user-name");

        Button logoutButton = new Button("Log out");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setOnAction(event -> showLogin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(
                16,
                logo,
                spacer,
                user,
                logoutButton
        );

        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        return header;
    }

    private void showTenantDashboard(
            ApiClient.AuthResponse authentication
    ) {
        navigationTargets.clear();
        VBox content = new VBox(22);
        content.setPadding(new Insets(32));

        Label loading = new Label("Loading usage data...");
        loading.getStyleClass().add("muted");
        content.getChildren().add(loading);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);

        BorderPane root = new BorderPane();
        root.setTop(createHeader(authentication));
        root.setLeft(createSidebar(
                "TENANT PORTAL",
                scrollPane,
                "Dashboard",
                "Live usage",
                "Billing",
                "Alerts"
        ));
        root.setCenter(scrollPane);
        root.getStyleClass().add("dashboard-background");

        Scene scene = new Scene(root, 1100, 720);
        applyStyles(scene);

        stage.setScene(scene);

        loadTenantDashboard(authentication, content);
    }

    private void loadTenantDashboard(
            ApiClient.AuthResponse authentication,
            VBox content
    ) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return apiClient.getUsageSummary(
                                authentication.tenantId(),
                                authentication.token()
                        );
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .whenComplete((summary, error)
                        -> Platform.runLater(() -> {
                    content.getChildren().clear();

                    if (error != null) {
                        showLoadingError(
                                content,
                                "Unable to load tenant dashboard.",
                                () -> loadTenantDashboard(
                                        authentication,
                                        content
                                )
                        );
                        return;
                    }

                    renderTenantDashboard(
                            authentication,
                            summary,
                            content
                    );
                })
                );
    }

    private void renderTenantDashboard(
            ApiClient.AuthResponse authentication,
            ApiClient.UsageSummaryResponse summary,
            VBox content
    ) {
        Label heading = new Label(summary.tenantName());
        heading.getStyleClass().add("page-title");
        Label alertsTitle = new Label("Live alerts");
        alertsTitle.getStyleClass().add("section-title");

        VBox alertPanel = createAlertPanel(summary.resources());
        Label subtitle = new Label(
                summary.planName()
                + " Plan • Billing month "
                + summary.billingMonth()
        );
        subtitle.getStyleClass().add("muted");

        VBox headingBox = new VBox(8, heading, subtitle);

        Button refreshButton = new Button("Refresh data");
        refreshButton.getStyleClass().add("primary-button");

        refreshButton.setOnAction(event -> {
            content.getChildren().setAll(
                    createLoadingLabel("Refreshing usage data...")
            );

            loadTenantDashboard(authentication, content);
        });

        HBox headingRow = createHeadingRow(
                headingBox,
                refreshButton
        );

        BigDecimal utilizedValue = summary.resources()
                .stream()
                .map(ApiClient.ResourceUsage::consumedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VBox monthlyPlanCard = createInfoCard(
                "Monthly plan",
                "₹" + summary.monthlyPrice()
        );

        VBox utilizedValueCard = createInfoCard(
                "Utilized value",
                "₹" + utilizedValue
        );

        VBox userRoleCard = createInfoCard(
                "Signed in as",
                authentication.role().replace("_", " ")
        );

        HBox overview = new HBox(
                18,
                monthlyPlanCard,
                utilizedValueCard,
                userRoleCard
        );

        GridPane resourceGrid = new GridPane();
        resourceGrid.setHgap(18);
        resourceGrid.setVgap(18);

        int index = 0;

        for (ApiClient.ResourceUsage resource : summary.resources()) {
            VBox card = createResourceCard(resource);

            resourceGrid.add(
                    card,
                    index % 2,
                    index / 2
            );

            index++;
        }

        Label resourceTitle = new Label("Resource usage");
        resourceTitle.getStyleClass().add("section-title");
        Label chartTitle = new Label("Usage analytics");
        chartTitle.getStyleClass().add("section-title");

        BarChart<String, Number> usageChart
                = createUsageChart(summary.resources());
        Label billingTitle = new Label("Billing summary");
        billingTitle.getStyleClass().add("section-title");

        VBox billingCard = createBillingCard(
                summary,
                utilizedValue
        );

        VBox invoiceDownload = createInvoiceDownload(
                authentication,
                summary
        );

        VBox usageSimulator = createUsageSimulator(
                authentication,
                content
        );

        navigationTargets.clear();
        navigationTargets.put("Dashboard", headingRow);
        navigationTargets.put("Live usage", usageSimulator);
        navigationTargets.put("Billing", billingTitle);
        navigationTargets.put("Alerts", alertsTitle);

        content.getChildren().addAll(
                headingRow,
                overview,
                usageSimulator,
                alertsTitle,
                alertPanel,
                resourceTitle,
                resourceGrid,
                chartTitle,
                usageChart,
                billingTitle,
                billingCard,
                invoiceDownload
        );
    }

    private VBox createInvoiceDownload(
            ApiClient.AuthResponse authentication,
            ApiClient.UsageSummaryResponse summary
    ) {
        Button downloadButton = new Button("Download PDF invoice");
        downloadButton.getStyleClass().add("primary-button");

        Label result = new Label();
        result.setWrapText(true);

        downloadButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Quota Flow invoice");
            fileChooser.setInitialFileName(
                    "quota-invoice-"
                            + summary.billingMonth()
                            + ".pdf"
            );
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "PDF document",
                            "*.pdf"
                    )
            );

            var destination = fileChooser.showSaveDialog(stage);
            if (destination == null) {
                return;
            }

            downloadButton.setDisable(true);
            downloadButton.setText("Downloading...");
            result.setText("");

            CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return apiClient.downloadInvoice(
                                    authentication.tenantId(),
                                    authentication.token(),
                                    destination.toPath()
                            );
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .whenComplete((savedPath, error) ->
                            Platform.runLater(() -> {
                                downloadButton.setDisable(false);
                                downloadButton.setText("Download PDF invoice");

                                if (error != null) {
                                    showFormError(result, rootMessage(error));
                                    return;
                                }

                                result.getStyleClass().setAll("success-label");
                                result.setText(
                                        "Invoice saved to: " + savedPath
                                );
                            })
                    );
        });

        VBox box = new VBox(10, downloadButton, result);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private void showPlatformAdminDashboard(
            ApiClient.AuthResponse authentication
    ) {
        navigationTargets.clear();
        VBox content = new VBox(22);
        content.setPadding(new Insets(32));

        content.getChildren().add(
                createLoadingLabel("Loading platform information...")
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);

        BorderPane root = new BorderPane();
        root.setTop(createHeader(authentication));
        root.setLeft(createSidebar(
                "ADMIN CONSOLE",
                scrollPane,
                "Overview",
                "Tenants",
                "Plans",
                "Compliance"
        ));
        root.setCenter(scrollPane);
        root.getStyleClass().add("dashboard-background");

        Scene scene = new Scene(root, 1200, 760);
        applyStyles(scene);

        stage.setScene(scene);

        loadPlatformAdminDashboard(authentication, content);
    }

    private void loadPlatformAdminDashboard(
            ApiClient.AuthResponse authentication,
            VBox content
    ) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        List<ApiClient.PlanResponse> plans
                                = apiClient.getPlans(authentication.token());

                        List<ApiClient.TenantResponse> tenants
                                = apiClient.getTenants(authentication.token());

                        ApiClient.AuditVerificationResponse audit
                                = apiClient.verifyAuditChain(
                                        authentication.token()
                                );

                        return new AdminDashboardData(
                                plans,
                                tenants,
                                audit
                        );
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .whenComplete((data, error)
                        -> Platform.runLater(() -> {
                    content.getChildren().clear();

                    if (error != null) {
                        showLoadingError(
                                content,
                                "Unable to load the platform dashboard.",
                                () -> loadPlatformAdminDashboard(
                                        authentication,
                                        content
                                )
                        );
                        return;
                    }

                    renderPlatformAdminDashboard(
                            authentication,
                            data,
                            content
                    );
                })
                );
    }

    private void renderPlatformAdminDashboard(
            ApiClient.AuthResponse authentication,
            AdminDashboardData data,
            VBox content
    ) {
        Label heading = new Label("Platform Administration");
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label(
                "Manage tenants, plans and platform compliance"
        );
        subtitle.getStyleClass().add("muted");

        VBox headingBox = new VBox(8, heading, subtitle);

        Button refreshButton = new Button("Refresh data");
        refreshButton.getStyleClass().add("primary-button");

        refreshButton.setOnAction(event -> {
            content.getChildren().setAll(
                    createLoadingLabel("Refreshing platform data...")
            );

            loadPlatformAdminDashboard(authentication, content);
        });

        HBox headingRow = createHeadingRow(headingBox, refreshButton);

        long activeTenants = data.tenants()
                .stream()
                .filter(tenant -> "ACTIVE".equals(tenant.status()))
                .count();

        VBox planCountCard = createInfoCard(
                "Subscription plans",
                String.valueOf(data.plans().size())
        );

        VBox tenantCountCard = createInfoCard(
                "Total tenants",
                String.valueOf(data.tenants().size())
        );

        VBox activeTenantCard = createInfoCard(
                "Active tenants",
                String.valueOf(activeTenants)
        );

        HBox overview = new HBox(
                18,
                planCountCard,
                tenantCountCard,
                activeTenantCard
        );

        Label managementTitle = new Label("Platform management");
        managementTitle.getStyleClass().add("section-title");

        HBox managementForms = new HBox(
                18,
                createPlanForm(authentication, content),
                createTenantForm(authentication, data.plans(), content)
        );
        managementForms.setAlignment(Pos.TOP_CENTER);

        Label auditTitle = new Label("Audit-chain verification");
        auditTitle.getStyleClass().add("section-title");

        VBox auditCard = createAuditCard(data.audit());

        Label tenantTitle = new Label("Registered tenants");
        tenantTitle.getStyleClass().add("section-title");

        VBox tenantTable = createTenantTable(data.tenants());

        Label planTitle = new Label("Subscription plans");
        planTitle.getStyleClass().add("section-title");

        VBox plansBox = new VBox(16);

        if (data.plans().isEmpty()) {
            plansBox.getChildren().add(
                    createEmptyCard("No subscription plans found.")
            );
        } else {
            for (ApiClient.PlanResponse plan : data.plans()) {
                plansBox.getChildren().add(createPlanCard(plan));
            }
        }

        navigationTargets.clear();
        navigationTargets.put("Overview", headingRow);
        navigationTargets.put("Tenants", tenantTitle);
        navigationTargets.put("Plans", planTitle);
        navigationTargets.put("Compliance", auditTitle);

        content.getChildren().addAll(
                headingRow,
                overview,
                managementTitle,
                managementForms,
                auditTitle,
                auditCard,
                tenantTitle,
                tenantTable,
                planTitle,
                plansBox
        );
    }

    private VBox createAuditCard(
            ApiClient.AuditVerificationResponse audit
    ) {
        Label heading = new Label(
                audit.valid()
                ? "AUDIT CHAIN VALID"
                : "AUDIT CHAIN INVALID"
        );

        heading.getStyleClass().addAll(
                "status-badge",
                audit.valid() ? "normal" : "critical"
        );

        Label message = new Label(audit.message());
        message.setWrapText(true);

        VBox card = new VBox(14, heading, message);
        card.getStyleClass().add("billing-card");

        return card;
    }

    private VBox createPlanForm(
            ApiClient.AuthResponse authentication,
            VBox content
    ) {
        Label title = new Label("Create subscription plan");
        title.getStyleClass().add("section-title");

        TextField name = new TextField();
        name.setPromptText("Plan name");
        TextField description = new TextField();
        description.setPromptText("Description");
        TextField price = new TextField();
        price.setPromptText("Monthly price");
        TextField apiLimit = new TextField();
        apiLimit.setPromptText("API request limit");
        TextField storageLimit = new TextField();
        storageLimit.setPromptText("Storage MB limit");
        TextField computeLimit = new TextField();
        computeLimit.setPromptText("Compute-second limit");
        TextField jobLimit = new TextField();
        jobLimit.setPromptText("Background-job limit");

        Label result = new Label();
        result.setWrapText(true);
        Button createButton = new Button("Create plan");
        createButton.getStyleClass().add("primary-button");

        createButton.setOnAction(event -> {
            if (name.getText().isBlank() || description.getText().isBlank()) {
                showFormError(result, "Plan name and description are required.");
                return;
            }

            BigDecimal monthlyPrice;
            long api;
            long storage;
            long compute;
            long jobs;

            try {
                monthlyPrice = new BigDecimal(price.getText().trim());
                api = Long.parseLong(apiLimit.getText().trim());
                storage = Long.parseLong(storageLimit.getText().trim());
                compute = Long.parseLong(computeLimit.getText().trim());
                jobs = Long.parseLong(jobLimit.getText().trim());
                if (monthlyPrice.signum() < 0 || api <= 0 || storage <= 0
                        || compute <= 0 || jobs <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                showFormError(result, "Enter a valid positive price and quota limits.");
                return;
            }

            createButton.setDisable(true);
            createButton.setText("Creating...");
            result.setText("");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.createPlan(
                            name.getText().trim(),
                            description.getText().trim(),
                            monthlyPrice,
                            api,
                            storage,
                            compute,
                            jobs,
                            authentication.token()
                    );
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }).whenComplete((createdPlan, error) -> Platform.runLater(() -> {
                createButton.setDisable(false);
                createButton.setText("Create plan");
                if (error != null) {
                    showFormError(result, rootMessage(error));
                    return;
                }
                content.getChildren().setAll(
                        createLoadingLabel("Plan created. Refreshing platform data...")
                );
                loadPlatformAdminDashboard(authentication, content);
            }));
        });

        VBox form = new VBox(
                12, title, name, description, price, apiLimit,
                storageLimit, computeLimit, jobLimit, createButton, result
        );
        form.getStyleClass().add("management-card");
        form.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(form, Priority.ALWAYS);
        return form;
    }

    private VBox createTenantForm(
            ApiClient.AuthResponse authentication,
            List<ApiClient.PlanResponse> plans,
            VBox content
    ) {
        Label title = new Label("Create tenant");
        title.getStyleClass().add("section-title");

        TextField name = new TextField();
        name.setPromptText("Company name");
        TextField slug = new TextField();
        slug.setPromptText("Slug, for example: nova-tech");

        ComboBox<String> planBox = new ComboBox<>();
        for (ApiClient.PlanResponse plan : plans) {
            planBox.getItems().add(plan.name());
        }
        if (!planBox.getItems().isEmpty()) {
            planBox.setValue(planBox.getItems().get(0));
        }
        planBox.setMaxWidth(Double.MAX_VALUE);
        planBox.setPromptText("Select subscription plan");

        TextField billingDay = new TextField("1");
        billingDay.setPromptText("Billing day (1-28)");
        Label result = new Label();
        result.setWrapText(true);
        Button createButton = new Button("Create tenant");
        createButton.getStyleClass().add("primary-button");
        createButton.setDisable(plans.isEmpty());

        if (plans.isEmpty()) {
            result.setText("Create a subscription plan first.");
            result.getStyleClass().setAll("muted");
        }

        Region formSpacer = new Region();
        VBox.setVgrow(formSpacer, Priority.ALWAYS);

        createButton.setOnAction(event -> {
            if (name.getText().isBlank() || slug.getText().isBlank()
                    || planBox.getValue() == null) {
                showFormError(result, "Company name, slug and plan are required.");
                return;
            }

            int day;
            try {
                day = Integer.parseInt(billingDay.getText().trim());
                if (day < 1 || day > 28) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                showFormError(result, "Billing day must be between 1 and 28.");
                return;
            }

            ApiClient.PlanResponse selectedPlan = plans.stream()
                    .filter(plan -> plan.name().equals(planBox.getValue()))
                    .findFirst()
                    .orElse(null);
            if (selectedPlan == null) {
                showFormError(result, "Select a valid subscription plan.");
                return;
            }

            createButton.setDisable(true);
            createButton.setText("Creating...");
            result.setText("");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return apiClient.createTenant(
                            name.getText().trim(),
                            slug.getText().trim(),
                            selectedPlan.id(),
                            day,
                            authentication.token()
                    );
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }).whenComplete((createdTenant, error) -> Platform.runLater(() -> {
                createButton.setDisable(false);
                createButton.setText("Create tenant");
                if (error != null) {
                    showFormError(result, rootMessage(error));
                    return;
                }
                content.getChildren().setAll(
                        createLoadingLabel("Tenant created. Refreshing platform data...")
                );
                loadPlatformAdminDashboard(authentication, content);
            }));
        });

        VBox form = new VBox(
                12, title, name, slug, planBox,
                billingDay, formSpacer, createButton, result
        );
        form.getStyleClass().add("management-card");
        form.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(form, Priority.ALWAYS);
        return form;
    }

    private void showFormError(Label label, String message) {
        label.getStyleClass().setAll("error-label");
        label.setText(message);
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null
                ? "The request could not be completed."
                : cause.getMessage();
    }

    private VBox createTenantTable(
            List<ApiClient.TenantResponse> tenants
    ) {
        VBox table = new VBox(0);
        table.getStyleClass().add("billing-card");

        GridPane header = new GridPane();
        header.setHgap(20);
        header.setPadding(new Insets(4, 8, 12, 8));

        Label nameHeader = createTableHeader("TENANT");
        Label slugHeader = createTableHeader("SLUG");
        Label planHeader = createTableHeader("PLAN");
        Label cycleHeader = createTableHeader("BILLING DAY");
        Label statusHeader = createTableHeader("STATUS");

        header.add(nameHeader, 0, 0);
        header.add(slugHeader, 1, 0);
        header.add(planHeader, 2, 0);
        header.add(cycleHeader, 3, 0);
        header.add(statusHeader, 4, 0);

        configureTenantColumns(
                nameHeader,
                slugHeader,
                planHeader,
                cycleHeader,
                statusHeader
        );

        table.getChildren().add(header);
        table.getChildren().add(new Separator());

        if (tenants.isEmpty()) {
            Label empty = new Label("No tenants found.");
            empty.getStyleClass().add("muted");
            VBox.setMargin(empty, new Insets(18, 8, 8, 8));
            table.getChildren().add(empty);
            return table;
        }

        for (ApiClient.TenantResponse tenant : tenants) {
            GridPane row = new GridPane();
            row.setHgap(20);
            row.setPadding(new Insets(14, 8, 14, 8));

            Label name = new Label(tenant.name());
            Label slug = new Label(tenant.slug());
            Label plan = new Label(tenant.planName());
            Label cycle = new Label(
                    String.valueOf(tenant.billingCycleDay())
            );

            Label status = new Label(tenant.status());
            status.getStyleClass().addAll(
                    "status-badge",
                    "ACTIVE".equals(tenant.status())
                    ? "normal"
                    : "critical"
            );

            row.add(name, 0, 0);
            row.add(slug, 1, 0);
            row.add(plan, 2, 0);
            row.add(cycle, 3, 0);
            row.add(status, 4, 0);

            configureTenantColumns(
                    name,
                    slug,
                    plan,
                    cycle,
                    status
            );

            table.getChildren().add(row);
            table.getChildren().add(new Separator());
        }

        return table;
    }

    private void configureTenantColumns(
            Region name,
            Region slug,
            Region plan,
            Region cycle,
            Region status
    ) {
        name.setPrefWidth(220);
        slug.setPrefWidth(200);
        plan.setPrefWidth(180);
        cycle.setPrefWidth(130);
        status.setPrefWidth(130);
    }

    private Label createTableHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("resource-name");
        return label;
    }

    private VBox createPlanCard(
            ApiClient.PlanResponse plan
    ) {
        Label name = new Label(plan.name());
        name.getStyleClass().add("section-title");

        Label status = new Label(
                plan.active() ? "ACTIVE" : "INACTIVE"
        );

        status.getStyleClass().addAll(
                "status-badge",
                plan.active() ? "normal" : "critical"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox heading = new HBox(
                12,
                name,
                spacer,
                status
        );

        heading.setAlignment(Pos.CENTER_LEFT);

        Label description = new Label(plan.description());
        description.getStyleClass().add("muted");
        description.setWrapText(true);

        Label price = new Label(
                "Monthly price: ₹" + plan.monthlyPrice()
        );
        price.getStyleClass().add("card-value");

        VBox quotaList = new VBox(8);

        for (ApiClient.QuotaResponse quota : plan.quotas()) {
            Label quotaLabel = new Label(
                    quota.resourceType().replace("_", " ")
                    + ": "
                    + String.format("%,d", quota.hardLimit())
                    + "  •  Warning "
                    + quota.warningPercentage()
                    + "%  •  Critical "
                    + quota.criticalPercentage()
                    + "%"
            );

            quotaList.getChildren().add(quotaLabel);
        }

        VBox card = new VBox(
                12,
                heading,
                description,
                price,
                new Separator(),
                quotaList
        );

        card.getStyleClass().add("billing-card");

        return card;
    }

    private HBox createHeadingRow(
            VBox headingBox,
            Button actionButton
    ) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(
                20,
                headingBox,
                spacer,
                actionButton
        );

        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox createSidebar(
            String title,
            ScrollPane scrollPane,
            String... items
    ) {
        Label heading = new Label(title);
        heading.getStyleClass().add("sidebar-title");
        VBox sidebar = new VBox(12, heading);

        Button firstButton = null;

        for (String item : items) {
            Button button = new Button(item);
            button.getStyleClass().addAll("sidebar-item", "sidebar-button");
            button.setMaxWidth(Double.MAX_VALUE);

            if (firstButton == null) {
                firstButton = button;
            }

            button.setOnAction(event -> {
                for (Node child : sidebar.getChildren()) {
                    child.getStyleClass().remove("sidebar-active");
                }

                button.getStyleClass().add("sidebar-active");

                Node target = navigationTargets.get(item);
                if (target != null) {
                    scrollToSection(scrollPane, target);
                }
            });

            sidebar.getChildren().add(button);
        }

        if (firstButton != null) {
            firstButton.getStyleClass().add("sidebar-active");
        }

        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    private void scrollToSection(
            ScrollPane scrollPane,
            Node target
    ) {
        Platform.runLater(() -> {
            Node content = scrollPane.getContent();
            double contentHeight = content.getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();
            double maximumScroll = contentHeight - viewportHeight;

            if (maximumScroll <= 0) {
                scrollPane.setVvalue(0);
                return;
            }

            double targetY = target.getBoundsInParent().getMinY();
            double position = targetY / maximumScroll;

            scrollPane.setVvalue(
                    Math.max(0, Math.min(1, position))
            );
        });
    }

    private Label createLoadingLabel(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("muted");
        return label;
    }

    private void showLoadingError(
            VBox content,
            String message,
            Runnable retryAction
    ) {
        Label errorLabel = new Label(
                message
                + " Ensure Docker and the backend are running."
        );

        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);

        Button retryButton = new Button("Try again");
        retryButton.getStyleClass().add("primary-button");
        retryButton.setOnAction(event -> retryAction.run());

        content.getChildren().addAll(
                errorLabel,
                retryButton
        );
    }

    private VBox createEmptyCard(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("muted");

        VBox card = new VBox(label);
        card.getStyleClass().add("billing-card");

        return card;
    }

    private VBox createInfoCard(
            String label,
            String value
    ) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("muted");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("card-value");

        VBox card = new VBox(
                8,
                labelNode,
                valueNode
        );

        card.getStyleClass().add("info-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(card, Priority.ALWAYS);

        return card;
    }

    private VBox createResourceCard(
            ApiClient.ResourceUsage resource
    ) {
        String resourceName = resource.resourceType()
                .replace("_", " ");

        Label name = new Label(resourceName);
        name.getStyleClass().add("resource-name");

        Label level = new Label(resource.level());
        level.getStyleClass().addAll(
                "status-badge",
                resource.level().toLowerCase()
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox cardHeader = new HBox(
                10,
                name,
                spacer,
                level
        );

        cardHeader.setAlignment(Pos.CENTER_LEFT);

        Label amount = new Label(
                String.format(
                        "%,d / %,d",
                        resource.used(),
                        resource.limit()
                )
        );

        amount.getStyleClass().add("usage-value");

        ProgressBar progressBar = new ProgressBar(
                Math.min(
                        resource.percentage() / 100,
                        1
                )
        );

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add(
                "progress-"
                + resource.level().toLowerCase()
        );

        Label percentage = new Label(
                String.format(
                        "%.1f%% used",
                        resource.percentage()
                )
        );

        percentage.getStyleClass().add("muted");

        VBox card = new VBox(
                14,
                cardHeader,
                amount,
                progressBar,
                percentage
        );

        card.getStyleClass().add("resource-card");
        card.setPrefWidth(470);

        return card;
    }

    private VBox createBillingCard(
            ApiClient.UsageSummaryResponse summary,
            BigDecimal utilizedValue
    ) {
        String invoiceNumber = "INV-"
                + summary.billingMonth().replace("-", "")
                + "-"
                + summary.tenantId()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        Label invoice = new Label(
                "Invoice: " + invoiceNumber
        );

        Label plan = new Label(
                "Subscription: " + summary.planName()
        );

        Label utilized = new Label(
                "Resource value utilized: ₹" + utilizedValue
        );

        Label payable = new Label(
                "Total payable: ₹" + summary.monthlyPrice()
        );
        payable.getStyleClass().add("billing-total");

        Label status = new Label("CURRENT");
        status.getStyleClass().addAll(
                "status-badge",
                "normal"
        );

        VBox card = new VBox(
                12,
                invoice,
                plan,
                utilized,
                payable,
                status
        );

        card.getStyleClass().add("billing-card");

        return card;
    }

    private void applyStyles(Scene scene) {
        scene.getStylesheets().add(
                getClass()
                        .getResource("/styles.css")
                        .toExternalForm()
        );
    }

    private record AdminDashboardData(
            List<ApiClient.PlanResponse> plans,
            List<ApiClient.TenantResponse> tenants,
            ApiClient.AuditVerificationResponse audit
            ) {

    }

    public static void main(String[] args) {
        launch(args);
    }
}
