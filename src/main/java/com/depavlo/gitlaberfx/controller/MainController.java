/*
 * MIT License
 *
 * Copyright (c) 2025 Pavlo Dehtiarov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.depavlo.gitlaberfx.controller;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.model.BranchModel;
import com.depavlo.gitlaberfx.model.UIStateModel;
import com.depavlo.gitlaberfx.service.GitLabService;
import com.depavlo.gitlaberfx.service.LocaleChangeService;
import com.depavlo.gitlaberfx.util.DialogHelper;
import com.depavlo.gitlaberfx.util.I18nUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Dependencies
    private AppConfig config;
    private GitLabService gitLabService;
    private Stage stage;
    private final UIStateModel uiStateModel = new UIStateModel();
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setDaemon(true);
        return thread;
    });

    // Task Management
    private final List<Future<?>> currentTasks = new ArrayList<>();
    private final AtomicBoolean pauseRequested = new AtomicBoolean(false);
    private CompletableFuture<Void> branchLoadFuture = CompletableFuture.completedFuture(null);

    // Listeners
    private ChangeListener<String> projectSelectionListener;
    private ChangeListener<String> targetBranchListener;


    // FXML Fields
    @FXML private ComboBox<String> projectComboBox;
    @FXML private ComboBox<String> destBranchComboBox;
    @FXML private TableView<BranchModel> branchesTableView;
    @FXML private TableColumn<BranchModel, Boolean> selectedColumn;
    @FXML private TableColumn<BranchModel, String> nameColumn;
    @FXML private TableColumn<BranchModel, String> lastCommitColumn;
    @FXML private TableColumn<BranchModel, Boolean> mergedColumn, mergeToDestColumn, protectedColumn, developersCanPushColumn, developersCanMergeColumn, canPushColumn, defaultColumn;
    @FXML private Label statusLabel, branchCounterLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button playButton, pauseButton, stopButton, rescanMergedButton;
    @FXML private Button refreshProjectsButton, refreshBranchesButton, selectAllButton, deselectAllButton, invertSelectionButton, deleteSelectedButton, mainDelMergedButton, mainDelUnmergedButton;

    public void initialize(AppConfig config, Stage stage) {
        this.config = config;
        this.stage = stage;
        this.gitLabService = new GitLabService(config);

        setupBindings();
        setupEventListeners();
        setupTableColumns();
        setupButtonBindings();
    }

    //<editor-fold desc="Initialization & Setup">
    private void setupBindings() {
        statusLabel.textProperty().bind(uiStateModel.statusMessageProperty());
        branchesTableView.setItems(uiStateModel.getCurrentProjectBranches());
    }

    private void setupEventListeners() {
        projectSelectionListener = (obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleProjectSelection(newVal);
            }
        };
        projectComboBox.valueProperty().addListener(projectSelectionListener);

        targetBranchListener = (obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleTargetBranchSelection(newVal);
            }
        };
        destBranchComboBox.valueProperty().addListener(targetBranchListener);

        uiStateModel.getCurrentProjectBranches().addListener((ListChangeListener<BranchModel>) c -> {
            updateBranchCounter();
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(branch -> branch.selectedProperty().addListener((o, ov, nv) -> updateBranchCounter()));
                }
            }
        });

        branchesTableView.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.SPACE) {
                BranchModel selectedBranch = branchesTableView.getSelectionModel().getSelectedItem();
                if (selectedBranch != null && !selectedBranch.isProtected()) {
                    selectedBranch.setSelected(!selectedBranch.isSelected());
                    event.consume();
                }
            }
        });
    }

    private void setupButtonBindings() {
        BooleanBinding isBusy = uiStateModel.busyProperty().not();

        BooleanBinding noProjectOrBranches = uiStateModel.currentProjectIdProperty().isNull()
                .or(Bindings.isEmpty(uiStateModel.getCurrentProjectBranches()));

        BooleanBinding noTargetBranch = uiStateModel.currentTargetBranchNameProperty().isNull();

        BooleanBinding noBranchSelected = Bindings.createBooleanBinding(() ->
                        uiStateModel.getCurrentProjectBranches().stream().noneMatch(BranchModel::isSelected),
                uiStateModel.getCurrentProjectBranches()
        );
        deleteSelectedButton.disableProperty().bind(isBusy.or(noProjectOrBranches).or(noBranchSelected));

        BooleanBinding noMergedBranchesFound = Bindings.createBooleanBinding(() ->
                        uiStateModel.getCurrentProjectBranches().stream().noneMatch(BranchModel::isMergedIntoTarget),
                uiStateModel.getCurrentProjectBranches()
        );
        mainDelMergedButton.disableProperty().bind(isBusy.or(noProjectOrBranches).or(noTargetBranch).or(noMergedBranchesFound));

        mainDelUnmergedButton.disableProperty().bind(isBusy.or(noProjectOrBranches).or(noTargetBranch));

        // Also bind other buttons to the busy state
        selectAllButton.disableProperty().bind(isBusy.or(noProjectOrBranches));
        deselectAllButton.disableProperty().bind(isBusy.or(noProjectOrBranches));
        invertSelectionButton.disableProperty().bind(isBusy.or(noProjectOrBranches));
        refreshProjectsButton.disableProperty().bind(isBusy);
        refreshBranchesButton.disableProperty().bind(isBusy.or(noProjectOrBranches));
        rescanMergedButton.disableProperty().bind(isBusy.or(noTargetBranch));
    }

    private void setupTableColumns() {
        branchesTableView.setEditable(true);
        selectedColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectedColumn.setCellFactory(column -> new CheckBoxTableCell<>());

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        lastCommitColumn.setCellValueFactory(new PropertyValueFactory<>("lastCommit"));

        setupBooleanColumn(mergedColumn, "merged", "column.tooltip.merged");
        setupBooleanColumn(mergeToDestColumn, "mergedIntoTarget", "column.tooltip.merged.into.target");
        setupBooleanColumn(protectedColumn, "protected", "column.tooltip.protected");
        setupBooleanColumn(developersCanPushColumn, "developersCanPush", "column.tooltip.developers.can.push");
        setupBooleanColumn(developersCanMergeColumn, "developersCanMerge", "column.tooltip.developers.can.merge");
        setupBooleanColumn(canPushColumn, "canPush", "column.tooltip.can.push");
        setupBooleanColumn(defaultColumn, "default", "column.tooltip.default");
    }

    private void setupBooleanColumn(TableColumn<BranchModel, Boolean> column, String propertyName, String tooltipKey) {
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item ? "✔" : " ");
                    setTooltip(new Tooltip(I18nUtil.getMessage(tooltipKey)));
                }
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
    }
    //</editor-fold>

    //<editor-fold desc="Core Logic: Project and Branch Loading">
    public CompletableFuture<Void> startInitialLoad() {
        if (!gitLabService.hasRequiredConfig()) {
            showWarning("warning.missing.settings", "warning.missing.settings.message");
            return CompletableFuture.completedFuture(null);
        }
        return refreshProjects();
    }

    @FXML
    public CompletableFuture<Void> refreshProjects() {
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        submitTask(I18nUtil.getMessage("main.status.loading.project.branches"), () -> {
            try {
                List<GitLabService.Project> projects = gitLabService.getProjects();
                Platform.runLater(() -> {
                    uiStateModel.setAllProjects(projects);
                    populateProjectComboBoxFromModel();
                    completionFuture.complete(null);
                });
            } catch (IOException e) {
                logger.error("Failed to load projects", e);
                Platform.runLater(() -> showError("app.error", e.getMessage()));
                completionFuture.completeExceptionally(e);
            }
        });
        return completionFuture;
    }

    private void handleProjectSelection(String selectedProjectName) {
        if (selectedProjectName == null) return;

        if (selectedProjectName.equals(getNotSelectedItemText())) {
            clearBranchView();
            return;
        }

        uiStateModel.getAllProjects().stream()
                .filter(p -> p.getPathName().equals(selectedProjectName))
                .findFirst()
                .ifPresent(project -> {
                    uiStateModel.setCurrentTargetBranchName(null);

                    uiStateModel.setCurrentProjectId(String.valueOf(project.getId()));
                    uiStateModel.setCurrentProjectName(project.getPathName());
                    this.branchLoadFuture = loadBranchesForProject(String.valueOf(project.getId()));
                });
    }

    private CompletableFuture<Void> loadBranchesForProject(String projectId) {
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        submitTask(I18nUtil.getMessage("main.status.updating.project.branches"), () -> {
            try {
                List<BranchModel> branches = gitLabService.getBranches(projectId);
                branches.sort((b1, b2) -> String.CASE_INSENSITIVE_ORDER.compare(b1.getName(), b2.getName()));

                Platform.runLater(() -> {
                    destBranchComboBox.valueProperty().removeListener(targetBranchListener);
                    uiStateModel.setCurrentProjectBranches(branches);
                    populateBranchComboBoxFromModel();
                    destBranchComboBox.valueProperty().addListener(targetBranchListener);

                    completionFuture.complete(null);
                });
            } catch (IOException e) {
                logger.error("Failed to load branches for project {}", projectId, e);
                Platform.runLater(() -> showError("app.error", e.getMessage()));
                completionFuture.completeExceptionally(e);
            }
        });
        return completionFuture;
    }

    private void handleTargetBranchSelection(String targetBranchName) {
        if (targetBranchName == null) return;

        if (targetBranchName.equals(getNotSelectedItemText())) {
            if (uiStateModel.getCurrentTargetBranchName() != null) {
                uiStateModel.getCurrentProjectBranches().forEach(b -> b.setMergedIntoTarget(false));
                uiStateModel.setCurrentTargetBranchName(null);
            }
        } else {
            uiStateModel.setCurrentTargetBranchName(targetBranchName);
            rescanMerged();
        }
    }

    @FXML
    private void rescanMerged() {
        String targetBranchName = uiStateModel.getCurrentTargetBranchName();
        if(targetBranchName == null) return;

        final List<BranchModel> branchesToCheck = new ArrayList<>(uiStateModel.getCurrentProjectBranches());
        final int total = branchesToCheck.size();

        submitTask(I18nUtil.getMessage("main.status.checking.merges"), () -> {
            for (int i = 0; i < total; i++) {
                if (Thread.currentThread().isInterrupted()) break;
                checkPause();

                BranchModel branch = branchesToCheck.get(i);
                final double progress = (double) (i + 1) / total;
                Platform.runLater(() -> updateProgress(progress));

                if (branch.getName().equals(targetBranchName)) {
                    Platform.runLater(() -> branch.setMergedIntoTarget(false));
                    continue;
                }

                try {
                    boolean isMerged = gitLabService.isCommitInMainBranch(uiStateModel.getCurrentProjectId(), branch.getName(), targetBranchName);
                    Platform.runLater(() -> branch.setMergedIntoTarget(isMerged));
                } catch (IOException e) {
                    logger.error("Error checking merge status for branch {}", branch.getName(), e);
                    Platform.runLater(() -> branch.setMergedIntoTarget(false));
                }
            }
        });
    }

    private void clearBranchView() {
        uiStateModel.clearProjectBranches();
        uiStateModel.setCurrentProjectId(null);
        uiStateModel.setCurrentProjectName(null);
        uiStateModel.setCurrentTargetBranchName(null);
        populateBranchComboBoxFromModel();
    }
    //</editor-fold>

    //<editor-fold desc="UI Update & Helper Methods">
    private void populateProjectComboBoxFromModel() {
        List<String> projectNames = new ArrayList<>();
        projectNames.add(getNotSelectedItemText());
        projectNames.addAll(uiStateModel.getAllProjects().stream()
                .map(GitLabService.Project::getPathName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList()));
        projectComboBox.setItems(FXCollections.observableArrayList(projectNames));
    }

    private void populateBranchComboBoxFromModel() {
        List<String> branchNames = new ArrayList<>();
        branchNames.add(getNotSelectedItemText());
        if (!uiStateModel.getCurrentProjectBranches().isEmpty()) {
            branchNames.addAll(uiStateModel.getCurrentProjectBranches().stream()
                    .map(BranchModel::getName)
                    .collect(Collectors.toList()));
        }
        destBranchComboBox.setItems(FXCollections.observableArrayList(branchNames));
        destBranchComboBox.setValue(getNotSelectedItemText());
    }

    private void updateBranchCounter() {
        long total = uiStateModel.getCurrentProjectBranches().size();
        long selected = uiStateModel.getCurrentProjectBranches().stream().filter(BranchModel::isSelected).count();
        Platform.runLater(() -> branchCounterLabel.setText(selected + "/" + total));
    }

    private void setUiBusy(boolean isBusy) {
        Platform.runLater(() -> {
            uiStateModel.setBusy(isBusy);

            // Major controls that are not bound should be disabled here
            projectComboBox.setDisable(isBusy);
            destBranchComboBox.setDisable(isBusy);
            branchesTableView.setDisable(isBusy);

            // Control buttons for tasks
            playButton.setDisable(true);
            pauseButton.setDisable(!isBusy);
            stopButton.setDisable(!isBusy);
            progressBar.setVisible(isBusy);

            if (!isBusy) {
                progressBar.setProgress(0);
                uiStateModel.setStatusMessage(I18nUtil.getMessage("app.ready"));
            }
        });
    }

    private void updateProgress(double progress) {
        progressBar.setProgress(progress);
    }

    private String getNotSelectedItemText() {
        return I18nUtil.getMessage("app.not.selected");
    }
    //</editor-fold>

    //<editor-fold desc="State Restoration for Locale Change">
    public void repopulateFromState(UIStateModel existingModel, LocaleChangeService.SavedState savedState) {
        this.uiStateModel.setAllProjects(existingModel.getAllProjects());
        this.uiStateModel.setCurrentProjectBranches(existingModel.getCurrentProjectBranches());
        this.uiStateModel.setCurrentProjectId(savedState.projectId);
        this.uiStateModel.setCurrentProjectName(savedState.projectName);
        this.uiStateModel.setCurrentTargetBranchName(savedState.targetBranchName);

        projectComboBox.valueProperty().removeListener(projectSelectionListener);
        destBranchComboBox.valueProperty().removeListener(targetBranchListener);

        populateProjectComboBoxFromModel();
        populateBranchComboBoxFromModel();

        String projectToSelect = this.uiStateModel.getCurrentProjectName();
        if (projectToSelect != null && projectComboBox.getItems().contains(projectToSelect)) {
            projectComboBox.setValue(projectToSelect);
        } else {
            projectComboBox.setValue(getNotSelectedItemText());
        }

        String branchToSelect = this.uiStateModel.getCurrentTargetBranchName();
        if (branchToSelect != null && destBranchComboBox.getItems().contains(branchToSelect)) {
            destBranchComboBox.setValue(branchToSelect);
        } else {
            destBranchComboBox.setValue(getNotSelectedItemText());
        }

        projectComboBox.valueProperty().addListener(projectSelectionListener);
        destBranchComboBox.valueProperty().addListener(targetBranchListener);
    }

    public void selectInitialProject() {
        projectComboBox.setValue(getNotSelectedItemText());
    }
    //</editor-fold>

    //<editor-fold desc="Deletion Logic">
    @FXML
    private void deleteSelected() {
        List<BranchModel> toDelete = uiStateModel.getCurrentProjectBranches().stream()
                .filter(BranchModel::isSelected)
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            executeBranchDeletion(toDelete, I18nUtil.getMessage("main.status.deleting.selected"));
        }
    }

    @FXML
    private void deleteMerged() {
        String targetBranch = uiStateModel.getCurrentTargetBranchName();
        if (targetBranch == null) {
            showError("error.target.branch", "error.target.branch.message");
            return;
        }
        LocalDate cutoffDate = DialogHelper.showDatePickerDialog(stage);
        if (cutoffDate != null) {
            List<BranchModel> toDelete = uiStateModel.getCurrentProjectBranches().stream()
                    .filter(b -> b.isMergedIntoTarget() && !b.isProtected())
                    .filter(b -> {
                        try {
                            return LocalDate.parse(b.getLastCommit().substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE).isBefore(cutoffDate);
                        } catch (DateTimeParseException e) {
                            return false;
                        }
                    }).collect(Collectors.toList());

            if(toDelete.isEmpty()) {
                showInfo("info.title", "info.no.merged.branches");
            } else {
                executeBranchDeletion(toDelete, I18nUtil.getMessage("main.status.deleting.merged"));
            }
        }
    }

    @FXML
    private void deleteUnmerged() {
        String targetBranch = uiStateModel.getCurrentTargetBranchName();
        if (targetBranch == null) {
            showError("error.target.branch", "error.target.branch.message");
            return;
        }
        LocalDate cutoffDate = DialogHelper.showDatePickerDialog(stage);
        if (cutoffDate != null) {
            List<BranchModel> toDelete = uiStateModel.getCurrentProjectBranches().stream()
                    .filter(b -> !b.isMergedIntoTarget() && !b.isProtected() && !b.getName().equals(targetBranch))
                    .filter(b -> {
                        try {
                            return LocalDate.parse(b.getLastCommit().substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE).isBefore(cutoffDate);
                        } catch (DateTimeParseException e) {
                            return false;
                        }
                    }).collect(Collectors.toList());

            if(toDelete.isEmpty()) {
                showInfo("info.title", "info.no.unmerged.branches");
            } else {
                executeBranchDeletion(toDelete, I18nUtil.getMessage("main.status.deleting.unmerged"));
            }
        }
    }

    private void executeBranchDeletion(List<BranchModel> branches, String operationDescription) {
        List<BranchModel> confirmed = DialogHelper.showDeleteConfirmationDialog(stage, branches);
        if (confirmed == null || confirmed.isEmpty()) return;

        final int total = confirmed.size();
        submitTask(operationDescription, () -> {
            for (int i = 0; i < total; i++) {
                if (Thread.currentThread().isInterrupted()) break;
                checkPause();

                BranchModel branch = confirmed.get(i);
                try {
                    Platform.runLater(() -> uiStateModel.setStatusMessage(I18nUtil.getMessage("main.status.deleting.branch", branch.getName())));
                    gitLabService.deleteBranch(uiStateModel.getCurrentProjectId(), branch.getName());
                } catch (IOException e) {
                    logger.error("Failed to delete branch {}", branch.getName(), e);
                }
                final double progress = (double) (i + 1) / total;
                Platform.runLater(() -> updateProgress(progress));
            }
            Platform.runLater(() -> loadBranchesForProject(uiStateModel.getCurrentProjectId()));
        });
    }
    //</editor-fold>

    //<editor-fold desc="UI Actions & Menu">
    @FXML private void refreshBranches() {
        if(uiStateModel.getCurrentProjectId() != null) {
            loadBranchesForProject(uiStateModel.getCurrentProjectId());
        }
    }
    @FXML private void selectAll() { uiStateModel.getCurrentProjectBranches().forEach(b -> b.setSelected(!b.isProtected())); }
    @FXML private void deselectAll() { uiStateModel.getCurrentProjectBranches().forEach(b -> b.setSelected(false)); }
    @FXML private void invertSelection() { uiStateModel.getCurrentProjectBranches().forEach(b -> { if(!b.isProtected()) b.setSelected(!b.isSelected()); }); }

    @FXML private void showSettings() {
        if (DialogHelper.showSettingsDialog(stage, config, this)) {
            this.gitLabService = new GitLabService(config);
            refreshProjects();
        }
    }

    @FXML private void showAbout() { DialogHelper.showAboutDialog(stage); }
    @FXML private void exit() { shutdown(); }

    public void changeLocale(Locale newLocale) {
        try {
            LocaleChangeService.changeLocale(newLocale, config, stage, this);
        } catch (Exception e) {
            logger.error("Failed to change locale", e);
            showError("app.error", "error.execution.message");
        }
    }
    //</editor-fold>

    //<editor-fold desc="Task & Thread Management">
    private void submitTask(String name, Runnable task) {
        logger.debug("Submitting task: {}", name);
        pauseRequested.set(false);
        setUiBusy(true);
        uiStateModel.setStatusMessage(name);

        Runnable wrappedTask = () -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Task '{}' failed", name, e);
                Platform.runLater(() -> showError("app.error", e.getMessage()));
            } finally {
                setUiBusy(false);
            }
        };
        currentTasks.removeIf(Future::isDone);
        currentTasks.add(executorService.submit(wrappedTask));
    }

    private void checkPause() {
        while (pauseRequested.get()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @FXML private void onPlayButtonClick() {
        pauseRequested.set(false);
        playButton.setDisable(true);
        pauseButton.setDisable(false);
        uiStateModel.setStatusMessage(I18nUtil.getMessage("status.execution.resumed"));
    }

    @FXML private void onPauseButtonClick() {
        pauseRequested.set(true);
        pauseButton.setDisable(true);
        playButton.setDisable(false);
        uiStateModel.setStatusMessage(I18nUtil.getMessage("status.execution.paused"));
    }

    @FXML private void onStopButtonClick() {
        currentTasks.forEach(task -> task.cancel(true));
        currentTasks.clear();
        pauseRequested.set(false);
        setUiBusy(false);
        uiStateModel.setStatusMessage(I18nUtil.getMessage("status.execution.stopped"));
    }

    public void shutdown() {
        onStopButtonClick();
        executorService.shutdownNow();
        Platform.exit();
    }
    //</editor-fold>

    //<editor-fold desc="Dialogs">
    private void showError(String titleKey, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18nUtil.getMessage(titleKey));
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String titleKey, String messageKey) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18nUtil.getMessage(titleKey));
            alert.setHeaderText(null);
            alert.setContentText(I18nUtil.getMessage(messageKey));
            alert.showAndWait();
        });
    }

    private void showWarning(String titleKey, String messageKey) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18nUtil.getMessage(titleKey));
            alert.setHeaderText(null);
            alert.setContentText(I18nUtil.getMessage(messageKey));
            alert.showAndWait();
        });
    }
    //</editor-fold>

    //<editor-fold desc="Getters for LocaleChangeService">
    public UIStateModel getUiStateModel() { return uiStateModel; }
    public CompletableFuture<Void> getBranchLoadFuture() { return branchLoadFuture; }
    //</editor-fold>
}
