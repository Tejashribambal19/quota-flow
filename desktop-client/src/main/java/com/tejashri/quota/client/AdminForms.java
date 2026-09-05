package com.tejashri.quota.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class AdminForms {
    private AdminForms() {}

    static void createTenant(Window owner, ApiClient api, String token,
                             List<ApiClient.PlanResponse> plans, Runnable onSaved) {
        Dialog<ButtonType> dialog = dialog(owner, "Create tenant");
        TextField name = new TextField();
        TextField slug = new TextField();
        ComboBox<ApiClient.PlanResponse> plan = new ComboBox<>(FXCollections.observableArrayList(plans));
        plan.setConverter(new javafx.util.StringConverter<>() {
            public String toString(ApiClient.PlanResponse value) { return value == null ? "" : value.name(); }
            public ApiClient.PlanResponse fromString(String value) { return null; }
        });
        if (!plans.isEmpty()) plan.getSelectionModel().selectFirst();
        Spinner<Integer> day = new Spinner<>(1, 28, 1);
        GridPane grid = grid();
        add(grid, 0, "Company name", name);
        add(grid, 1, "Slug", slug);
        add(grid, 2, "Plan", plan);
        add(grid, 3, "Billing day", day);
        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(ignored -> run(
                () -> api.createTenant(name.getText().trim(), slug.getText().trim(),
                        plan.getValue().id(), day.getValue(), token), onSaved));
    }

    static void createPlan(Window owner, ApiClient api, String token, Runnable onSaved) {
        Dialog<ButtonType> dialog = dialog(owner, "Create subscription plan");
        TextField name = new TextField();
        TextField description = new TextField();
        TextField price = new TextField("1499.00");
        TextField apiLimit = new TextField("20000");
        TextField storageLimit = new TextField("10000");
        TextField computeLimit = new TextField("50000");
        TextField jobLimit = new TextField("5000");
        GridPane grid = grid();
        add(grid, 0, "Plan name", name);
        add(grid, 1, "Description", description);
        add(grid, 2, "Monthly price", price);
        add(grid, 3, "API requests", apiLimit);
        add(grid, 4, "Storage MB", storageLimit);
        add(grid, 5, "Compute seconds", computeLimit);
        add(grid, 6, "Background jobs", jobLimit);
        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(ignored -> run(
                () -> api.createPlan(name.getText().trim(), description.getText().trim(),
                        new BigDecimal(price.getText()), Long.parseLong(apiLimit.getText()),
                        Long.parseLong(storageLimit.getText()), Long.parseLong(computeLimit.getText()),
                        Long.parseLong(jobLimit.getText()), token), onSaved));
    }

    private static Dialog<ButtonType> dialog(Window owner, String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        return dialog;
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));
        return grid;
    }

    private static void add(GridPane grid, int row, String label, Control input) {
        input.setMaxWidth(Double.MAX_VALUE);
        grid.addRow(row, new Label(label), input);
    }

    private static void run(ThrowingAction action, Runnable onSaved) {
        CompletableFuture.runAsync(() -> {
            try { action.run(); }
            catch (Exception ex) { throw new RuntimeException(ex); }
        }).whenComplete((ignored, error) -> Platform.runLater(() -> {
            if (error == null) onSaved.run();
            else new Alert(Alert.AlertType.ERROR, "Could not save: " + rootMessage(error)).showAndWait();
        }));
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage();
    }

    @FunctionalInterface
    private interface ThrowingAction { void run() throws Exception; }
}
