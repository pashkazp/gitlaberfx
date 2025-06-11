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
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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

/**
 * Main controller for the GitLaberFX application.
 * This class is responsible for managing the main UI of the application,
 * handling user interactions, and coordinating operations with GitLab.
 * It provides functionality for:
 * - Loading and displaying GitLab projects and branches
 * - Managing branch selection and filtering
 * - Checking merge status of branches
 * - Deleting branches (selected, merged, or unmerged)
 * - Managing background tasks with pause/resume/stop capabilities
 * - Handling locale changes
 * 
 * The controller uses JavaFX properties and bindings for reactive UI updates
 * and executes potentially long-running operations in background threads
 * to keep the UI responsive.
 */
public class MainController {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Dependencies
    /** Application configuration containing GitLab URL, API key, and locale settings. */
    private AppConfig config;

    /** Service for interacting with the GitLab API. */
    private GitLabService gitLabService;

    /** The main application stage. */
    private Stage stage;

    /** Model containing the current UI state (projects, branches, selection state). */
    private final UIStateModel uiStateModel = new UIStateModel();

    /** Thread pool for executing background tasks. */
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setDaemon(true);
        return thread;
    });

    // Task Management
    /** List of currently running background tasks. */
    private final List<Future<?>> currentTasks = new ArrayList<>();

    /** Flag indicating whether task execution is currently paused. */
    private final AtomicBoolean pauseRequested = new AtomicBoolean(false);

    /** Future representing the completion of the branch loading operation. */
    private CompletableFuture<Void> branchLoadFuture = CompletableFuture.completedFuture(null);

    // Listeners
    /** Listener for project selection changes in the UI. */
    private ChangeListener<String> projectSelectionListener;

    /** Listener for target branch selection changes in the UI. */
    private ChangeListener<String> targetBranchListener;


    // FXML Fields
    /** ComboBox for selecting a GitLab project. */
    @FXML private ComboBox<String> projectComboBox;

    /** ComboBox for selecting a target branch for merge status checking. */
    @FXML private ComboBox<String> destBranchComboBox;

    /** TableView displaying the branches of the selected project. */
    @FXML private TableView<BranchModel> branchesTableView;

    /** TableColumn for the branch selection checkboxes. */
    @FXML private TableColumn<BranchModel, Boolean> selectedColumn;

    /** TableColumn for the branch names. */
    @FXML private TableColumn<BranchModel, String> nameColumn;

    /** TableColumn for the last commit information. */
    @FXML private TableColumn<BranchModel, String> lastCommitColumn;

    /** TableColumns for various branch properties (merged, merged into target, protected, etc.). */
    @FXML private TableColumn<BranchModel, Boolean> mergedColumn, mergeToDestColumn, protectedColumn, 
                                                    developersCanPushColumn, developersCanMergeColumn, 
                                                    canPushColumn, defaultColumn;

    /** Labels for status messages and branch counter. */
    @FXML private Label statusLabel, branchCounterLabel;

    /** Progress bar for displaying operation progress. */
    @FXML private ProgressBar progressBar;

    /** Buttons for controlling task execution (play, pause, stop) and rescanning merged branches. */
    @FXML private Button playButton, pauseButton, stopButton, rescanMergedButton;

    /** Buttons for various operations (refresh, selection, deletion). */
    @FXML private Button refreshProjectsButton, refreshBranchesButton, selectAllButton, 
                         deselectAllButton, invertSelectionButton, deleteSelectedButton,
                         mainDelMergedButton, mainDelUnmergedButton,
                         archiveSelectedButton, mainArchiveMergedButton, mainArchiveUnmergedButton;

    /**
     * Initializes the controller with the application configuration and stage.
     * This method is called after the FXML has been loaded.
     * It sets up the UI components, bindings, event listeners, and table columns.
     *
     * @param config The application configuration containing GitLab URL and API key
     * @param stage The main application stage
     */
    public void initialize(AppConfig config, Stage stage) {
        this.config = config;
        this.stage = stage;
        this.gitLabService = new GitLabService(config);

        setupBindings();
        setupEventListeners();
        setupTableColumns();
        setupButtonBindings();
        setUiBusy(false);
        setupTooltips(); // Set tooltips for UI elements
    }

/**
     * Set up and initializes tips (Tooltips) for different items
     * User interface in the main window.
     *
     * This method is commonly called during the controller initialization
     * To increase the convenience of using by providing additional
     * Information on the functionality of the buttons, input fields or other components.
     *
     * Tips can be downloaded from resource files to support
     * Internationalization.
     */
    private void setupTooltips() {
        projectComboBox.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.project.combobox")));
        destBranchComboBox.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.target.branch.combobox")));
        refreshProjectsButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.refresh.projects")));
        refreshBranchesButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.refresh.branches")));
        selectAllButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.select.all")));
        deselectAllButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.deselect.all")));
        invertSelectionButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.invert.selection")));
        deleteSelectedButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.delete.selected")));
        mainDelMergedButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.delete.merged")));
        mainDelUnmergedButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.delete.unmerged")));
        archiveSelectedButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.archive.selected")));
        mainArchiveMergedButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.archive.merged")));
        mainArchiveUnmergedButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.archive.unmerged")));

        playButton.setTooltip(new Tooltip(I18nUtil.getMessage("button.tooltip.play")));
        pauseButton.setTooltip(new Tooltip(I18nUtil.getMessage("button.tooltip.pause")));
        stopButton.setTooltip(new Tooltip(I18nUtil.getMessage("button.tooltip.stop")));
        rescanMergedButton.setTooltip(new Tooltip(I18nUtil.getMessage("button.tooltip.rescan")));
    }

    //<editor-fold desc="Initialization & Setup">
    /**
     * Sets up data bindings between UI components and the state model.
     * This method binds the status label text to the status message property
     * and sets the branches table items to the current project branches.
     */
    private void setupBindings() {
        statusLabel.textProperty().bind(uiStateModel.statusMessageProperty());
        branchesTableView.setItems(uiStateModel.getCurrentProjectBranches());
    }

    /**
     * Sets up event listeners for UI components.
     * This method creates and attaches listeners for:
     * - Project selection changes
     * - Target branch selection changes
     * - Branch list changes
     * - Keyboard events on the branches table
     */
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

    /**
     * Sets up bindings for button disable properties.
     * This method creates complex bindings that disable buttons based on the current state:
     * - Whether the application is busy
     * - Whether a project is selected
     * - Whether branches are available
     * - Whether a target branch is selected
     * - Whether any branches are selected
     * - Whether any merged branches are found
     */
    private void setupButtonBindings() {
        BooleanProperty isBusy = uiStateModel.busyProperty();

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
        archiveSelectedButton.disableProperty().bind(isBusy.or(noProjectOrBranches).or(noBranchSelected));
        mainArchiveMergedButton.disableProperty().bind(isBusy.or(noProjectOrBranches).or(noTargetBranch).or(noMergedBranchesFound));
        mainArchiveUnmergedButton.disableProperty().bind(isBusy.or(noProjectOrBranches).or(noTargetBranch));

        selectAllButton.disableProperty().bind(isBusy.or(noProjectOrBranches));
        deselectAllButton.disableProperty().bind(isBusy.or(noProjectOrBranches));
        invertSelectionButton.disableProperty().bind(isBusy.or(noProjectOrBranches));
        refreshProjectsButton.disableProperty().bind(isBusy);
        refreshBranchesButton.disableProperty().bind(isBusy.or(noProjectOrBranches));
        rescanMergedButton.disableProperty().bind(isBusy.or(noTargetBranch));
    }

    /**
     * Sets up the table columns for the branches table.
     * This method configures:
     * - Cell value factories to bind to branch model properties
     * - Cell factories for custom rendering
     * - Boolean columns with custom rendering and tooltips
     */
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

    /**
     * Sets up a boolean column with custom rendering and tooltip.
     * This method configures a table column to display boolean values as checkmarks
     * and adds a tooltip with the specified message.
     *
     * @param column The table column to configure
     * @param propertyName The name of the property in the BranchModel to bind to
     * @param tooltipKey The key for the tooltip message in the resource bundle
     */
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
    /**
     * Starts the initial data loading process.
     * This method is called when the application starts or when settings are changed.
     * It checks if the GitLab configuration is valid and then refreshes the projects list.
     *
     * @return A CompletableFuture that completes when the initial load is done
     */
    public CompletableFuture<Void> startInitialLoad() {
        if (!config.isConfigurationValid()) {
            showWarning("warning.missing.settings", "warning.missing.settings.message");
            return CompletableFuture.completedFuture(null);
        }
        return refreshProjects();
    }

    /**
     * Refreshes the list of GitLab projects.
     * This method fetches the projects from GitLab and updates the UI.
     * It can be triggered by the user via the refresh button or programmatically.
     *
     * @return A CompletableFuture that completes when the projects are loaded
     */
    @FXML
    public CompletableFuture<Void> refreshProjects() {
        if (!config.isConfigurationValid()) {
            showWarning("warning.missing.settings", "warning.missing.settings.message");
            return CompletableFuture.completedFuture(null);
        }
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
                Platform.runLater(() -> showError("error.loading", I18nUtil.getMessage("error.loading.projects.generic")));
                completionFuture.completeExceptionally(e);
            }
        });
        return completionFuture;
    }

    /**
     * Handles the selection of a project in the UI.
     * This method is called when the user selects a project from the dropdown.
     * It updates the state model with the selected project and loads its branches.
     *
     * @param selectedProjectName The name of the selected project
     */
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

    /**
     * Loads the branches for a specific project.
     * This method fetches the branches from GitLab and updates the UI.
     * It is called when a project is selected or when branches are refreshed.
     *
     * @param projectId The ID of the project to load branches for
     * @return A CompletableFuture that completes when the branches are loaded
     */
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
                Platform.runLater(() -> showError("error.loading", I18nUtil.getMessage("error.loading.branches.generic")));
                completionFuture.completeExceptionally(e);
            }
        });
        return completionFuture;
    }

    /**
     * Handles the selection of a target branch in the UI.
     * This method is called when the user selects a target branch from the dropdown.
     * It updates the state model with the selected target branch and rescans merge status.
     *
     * @param targetBranchName The name of the selected target branch
     */
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

    /**
     * Rescans the merge status of all branches against the target branch.
     * This method checks each branch to determine if it has been merged into the target branch.
     * It updates the mergedIntoTarget property of each branch accordingly.
     * This operation can be time-consuming for repositories with many branches.
     */
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

    /**
     * Clears the branch view and resets related state.
     * This method is called when no project is selected or when the project selection is cleared.
     * It clears the branches list, resets project and branch selection state, and updates the UI.
     */
    private void clearBranchView() {
        uiStateModel.clearProjectBranches();
        uiStateModel.setCurrentProjectId(null);
        uiStateModel.setCurrentProjectName(null);
        uiStateModel.setCurrentTargetBranchName(null);
        populateBranchComboBoxFromModel();
    }

    /**
     * Removes deleted branches from the model.
     * This method is called after branches are deleted to update the model without reloading all branches.
     * It preserves the merge markers and other properties of the remaining branches.
     *
     * @param deletedBranches The list of branches that were deleted
     */
    private void removeDeletedBranchesFromModel(List<BranchModel> deletedBranches) {
        // Get the current list of branches
        ObservableList<BranchModel> currentBranches = uiStateModel.getCurrentProjectBranches();

        // Remove the deleted branches one by one
        for (BranchModel branch : new ArrayList<>(deletedBranches)) {
            currentBranches.removeIf(b -> b.getName().equals(branch.getName()));
        }

        // Update the branch counter
        updateBranchCounter();
    }

    /**
     * Updates the model after branches are archived by renaming them locally.
     *
     * @param oldName  Original branch name
     * @param newName  New archived branch name
     */
    private void updateBranchNameInModel(String oldName, String newName) {
        for (BranchModel b : uiStateModel.getCurrentProjectBranches()) {
            if (b.getName().equals(oldName)) {
                b.setName(newName);
                break;
            }
        }
        updateBranchCounter();
    }
    //</editor-fold>

    //<editor-fold desc="UI Update & Helper Methods">
    /**
     * Populates the project combo box with project names from the model.
     * This method creates a list of project names, adds a "Not Selected" item at the top,
     * sorts the list, and sets it as the items of the project combo box.
     */
    private void populateProjectComboBoxFromModel() {
        List<String> projectNames = new ArrayList<>();
        projectNames.add(getNotSelectedItemText());
        projectNames.addAll(uiStateModel.getAllProjects().stream()
                .map(GitLabService.Project::getPathName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList()));
        projectComboBox.setItems(FXCollections.observableArrayList(projectNames));
    }

    /**
     * Populates the branch combo box with branch names from the model.
     * This method creates a list of branch names, adds a "Not Selected" item at the top,
     * and sets it as the items of the branch combo box.
     * It also sets the selected value to "Not Selected".
     */
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

    /**
     * Updates the branch counter label with the current count of selected branches.
     * This method calculates the total number of branches and the number of selected branches,
     * and updates the branch counter label with the format "selected/total".
     */
    private void updateBranchCounter() {
        long total = uiStateModel.getCurrentProjectBranches().size();
        long selected = uiStateModel.getCurrentProjectBranches().stream().filter(BranchModel::isSelected).count();
        Platform.runLater(() -> branchCounterLabel.setText(selected + "/" + total));
    }

    /**
     * Sets the UI busy state.
     * This method updates the UI to reflect whether a background task is running.
     * When busy, it disables user input controls and shows the progress bar.
     * When not busy, it enables user input controls, hides the progress bar, and resets the status message.
     *
     * @param isBusy true if the UI should be in the busy state, false otherwise
     */
    private void setUiBusy(boolean isBusy) {
        Platform.runLater(() -> {
            uiStateModel.setBusy(isBusy);

            projectComboBox.setDisable(isBusy);
            destBranchComboBox.setDisable(isBusy);
            branchesTableView.setDisable(isBusy);

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

    /**
     * Updates the progress bar with the specified progress value.
     * This method is called during long-running operations to show progress to the user.
     *
     * @param progress the progress value between 0.0 and 1.0
     */
    private void updateProgress(double progress) {
        progressBar.setProgress(progress);
    }

    /**
     * Gets the localized text for the "Not Selected" item in combo boxes.
     * This method retrieves the text from the resource bundle.
     *
     * @return the localized text for "Not Selected"
     */
    private String getNotSelectedItemText() {
        return I18nUtil.getMessage("app.not.selected");
    }
    //</editor-fold>

    //<editor-fold desc="State Restoration for Locale Change">
    /**
     * Repopulates the UI state from a saved state after a locale change.
     * This method is called by the LocaleChangeService after the UI has been reloaded
     * with a new locale. It restores the state of the UI components and the state model
     * from the saved state, ensuring that the user's context is preserved across locale changes.
     *
     * @param existingModel The existing UI state model containing projects and branches
     * @param savedState The saved state containing selected project and branch information
     */
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

    /**
     * Selects the initial project in the UI.
     * This method is called after the initial data load is complete.
     * It sets the project combo box to the "Not Selected" value,
     * which triggers the project selection listener to clear the branch view.
     */
    public void selectInitialProject() {
        projectComboBox.setValue(getNotSelectedItemText());
    }
    //</editor-fold>

    //<editor-fold desc="Deletion Logic">
    /**
     * Deletes the selected branches.
     * This method collects all branches that are currently selected in the UI
     * and initiates the deletion process for them.
     * It is triggered by the "Delete Selected" button.
     */
    @FXML
    private void deleteSelected() {
        List<BranchModel> toDelete = uiStateModel.getCurrentProjectBranches().stream()
                .filter(BranchModel::isSelected)
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            executeBranchDeletion(toDelete, I18nUtil.getMessage("main.status.deleting.selected"));
        }
    }

    /**
     * Archives the selected branches by renaming them with the <code>archive/</code> prefix.
     */
    @FXML
    private void archiveSelected() {
        List<BranchModel> toArchive = uiStateModel.getCurrentProjectBranches().stream()
                .filter(BranchModel::isSelected)
                .collect(Collectors.toList());
        if (!toArchive.isEmpty()) {
            executeBranchArchiving(toArchive, I18nUtil.getMessage("main.archive.selected"));
        }
    }

    /**
     * Deletes merged branches older than a specified date.
     * This method prompts the user to select a cutoff date, then collects all branches
     * that are merged into the target branch, not protected, and older than the cutoff date.
     * It then initiates the deletion process for these branches.
     * It is triggered by the "Delete Merged" button.
     */
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

    /**
     * Archives merged branches older than the selected date.
     */
    @FXML
    private void archiveMerged() {
        String targetBranch = uiStateModel.getCurrentTargetBranchName();
        if (targetBranch == null) {
            showError("error.target.branch", "error.target.branch.message");
            return;
        }
        LocalDate cutoffDate = DialogHelper.showDatePickerDialog(stage);
        if (cutoffDate != null) {
            List<BranchModel> toArchive = uiStateModel.getCurrentProjectBranches().stream()
                    .filter(b -> b.isMergedIntoTarget() && !b.isProtected())
                    .filter(b -> {
                        try {
                            return LocalDate.parse(b.getLastCommit().substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE).isBefore(cutoffDate);
                        } catch (DateTimeParseException e) {
                            return false;
                        }
                    }).collect(Collectors.toList());

            if (toArchive.isEmpty()) {
                showInfo("info.title", "info.no.merged.branches");
            } else {
                executeBranchArchiving(toArchive, I18nUtil.getMessage("main.archive.merged"));
            }
        }
    }

    /**
     * Deletes unmerged branches older than a specified date.
     * This method prompts the user to select a cutoff date, then collects all branches
     * that are not merged into the target branch, not protected, not the target branch itself,
     * and older than the cutoff date.
     * It then initiates the deletion process for these branches.
     * It is triggered by the "Delete Unmerged" button.
     */
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

    /**
     * Archives unmerged branches older than the selected date.
     */
    @FXML
    private void archiveUnmerged() {
        String targetBranch = uiStateModel.getCurrentTargetBranchName();
        if (targetBranch == null) {
            showError("error.target.branch", "error.target.branch.message");
            return;
        }
        LocalDate cutoffDate = DialogHelper.showDatePickerDialog(stage);
        if (cutoffDate != null) {
            List<BranchModel> toArchive = uiStateModel.getCurrentProjectBranches().stream()
                    .filter(b -> !b.isMergedIntoTarget() && !b.isProtected() && !b.getName().equals(targetBranch))
                    .filter(b -> {
                        try {
                            return LocalDate.parse(b.getLastCommit().substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE).isBefore(cutoffDate);
                        } catch (DateTimeParseException e) {
                            return false;
                        }
                    }).collect(Collectors.toList());

            if (toArchive.isEmpty()) {
                showInfo("info.title", "info.no.unmerged.branches");
            } else {
                executeBranchArchiving(toArchive, I18nUtil.getMessage("main.archive.unmerged"));
            }
        }
    }

    /**
     * Executes the branch deletion process.
     * This method shows a confirmation dialog, then deletes the confirmed branches one by one.
     * It updates the progress bar and status message during the operation.
     * After all branches are deleted, it removes the deleted branches from the model
     * while preserving the target branch selection and merge markers.
     *
     * @param branches The list of branches to delete
     * @param operationDescription A description of the operation for the status message
     */
    private void executeBranchDeletion(List<BranchModel> branches, String operationDescription) {
        List<BranchModel> confirmed = DialogHelper.showDeleteConfirmationDialog(stage, branches,
                                                                              operationDescription,
                                                                              uiStateModel.getCurrentProjectName());
        if (confirmed == null || confirmed.isEmpty()) return;

        // Save the current target branch name
        final String savedTargetBranchName = uiStateModel.getCurrentTargetBranchName();

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

            // Instead of reloading all branches, just remove the deleted ones
            Platform.runLater(() -> {
                removeDeletedBranchesFromModel(confirmed);

                // Ensure the target branch is still selected
                if (uiStateModel.getCurrentTargetBranchName() == null && savedTargetBranchName != null) {
                    uiStateModel.setCurrentTargetBranchName(savedTargetBranchName);
                    populateBranchComboBoxFromModel();
                    destBranchComboBox.setValue(savedTargetBranchName);
                }
            });
        });
    }

    /**
     * Executes the branch archiving process. Branches are renamed to
     * <code>archive/&lt;branch&gt;</code> without reloading from GitLab.
     *
     * @param branches            The list of branches to archive
     * @param operationDescription Description key for status messages
     */
    private void executeBranchArchiving(List<BranchModel> branches, String operationDescription) {
        List<BranchModel> confirmed = DialogHelper.showDeleteConfirmationDialog(stage, branches,
                                                                               operationDescription,
                                                                               uiStateModel.getCurrentProjectName());
        if (confirmed == null || confirmed.isEmpty()) return;

        final String savedTargetBranchName = uiStateModel.getCurrentTargetBranchName();

        final int total = confirmed.size();
        submitTask(operationDescription, () -> {
            for (int i = 0; i < total; i++) {
                if (Thread.currentThread().isInterrupted()) break;
                checkPause();

                BranchModel branch = confirmed.get(i);
                String oldName = branch.getName();
                String newName = "archive/" + oldName;
                try {
                    String msg = I18nUtil.getMessage("main.status.archiving.branch", oldName);
                    Platform.runLater(() -> uiStateModel.setStatusMessage(msg));
                    gitLabService.archiveBranch(uiStateModel.getCurrentProjectId(), oldName);
                } catch (IOException e) {
                    logger.error("Failed to archive branch {}", oldName, e);
                    continue;
                }
                String finalNewName = newName;
                Platform.runLater(() -> updateBranchNameInModel(oldName, finalNewName));
                final double progress = (double) (i + 1) / total;
                Platform.runLater(() -> updateProgress(progress));
            }

            Platform.runLater(() -> {
                populateBranchComboBoxFromModel();
                if (savedTargetBranchName != null && destBranchComboBox.getItems().contains(savedTargetBranchName)) {
                    destBranchComboBox.setValue(savedTargetBranchName);
                }
            });
        });
    }
    //</editor-fold>

    //<editor-fold desc="UI Actions & Menu">
    /**
     * Refreshes the branches for the current project.
     * This method reloads the branches from GitLab for the currently selected project.
     * It is triggered by the "Refresh Branches" button.
     */
    @FXML private void refreshBranches() {
        if (!config.isConfigurationValid()) {
            showWarning("warning.missing.settings", "warning.missing.settings.message");
            return;
        }
        if(uiStateModel.getCurrentProjectId() != null) {
            loadBranchesForProject(uiStateModel.getCurrentProjectId());
        }
    }

    /**
     * Selects all non-protected branches in the branches table.
     * This method sets the selected property to true for all branches that are not protected.
     * It is triggered by the "Select All" button.
     */
    @FXML private void selectAll() { uiStateModel.getCurrentProjectBranches().forEach(b -> b.setSelected(!b.isProtected())); }

    /**
     * Deselects all branches in the branches table.
     * This method sets the selected property to false for all branches.
     * It is triggered by the "Deselect All" button.
     */
    @FXML private void deselectAll() { uiStateModel.getCurrentProjectBranches().forEach(b -> b.setSelected(false)); }

    /**
     * Inverts the selection of all non-protected branches in the branches table.
     * This method toggles the selected property for all branches that are not protected.
     * It is triggered by the "Invert Selection" button.
     */
    @FXML private void invertSelection() { uiStateModel.getCurrentProjectBranches().forEach(b -> { if(!b.isProtected()) b.setSelected(!b.isSelected()); }); }

    /**
     * Shows the settings dialog.
     * This method displays the settings dialog, allowing the user to configure
     * the GitLab URL, API key, and application language.
     * If the settings are saved, it reinitializes the GitLab service and reloads the data.
     * It is triggered by the "Settings" menu item.
     */
    @FXML private void showSettings() {
        if (DialogHelper.showSettingsDialog(stage, config, this)) {
            this.gitLabService = new GitLabService(config);
            startInitialLoad();
        }
    }

    /**
     * Shows the about dialog.
     * This method displays the about dialog, showing information about the application.
     * It is triggered by the "About" menu item.
     */
    @FXML private void showAbout() { DialogHelper.showAboutDialog(stage); }

    /**
     * Exits the application.
     * This method shuts down the application, stopping all background tasks.
     * It is triggered by the "Exit" menu item.
     */
    @FXML private void exit() { shutdown(); }

    /**
     * Changes the application locale.
     * This method changes the language of the application UI.
     * It uses the LocaleChangeService to reload the UI with the new locale
     * while preserving the current state.
     *
     * @param newLocale The new locale to set
     */
    public void changeLocale(Locale newLocale) {
        try {
            LocaleChangeService.changeLocale(newLocale, config, stage, this);
        } catch (Exception e) {
            logger.error("Failed to change locale", e);
            showError("app.error", I18nUtil.getMessage("error.execution.generic"));
        }
    }
    //</editor-fold>

    //<editor-fold desc="Task & Thread Management">
    /**
     * Submits a task for execution in a background thread.
     * This method wraps the task with error handling and UI state management,
     * then submits it to the executor service.
     * It sets the UI to the busy state while the task is running and updates the status message.
     *
     * @param name A description of the task for the status message
     * @param task The task to execute
     */
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
                Platform.runLater(() -> showError("app.error", I18nUtil.getMessage("error.execution.generic")));
            } finally {
                setUiBusy(false);
            }
        };
        currentTasks.removeIf(Future::isDone);
        currentTasks.add(executorService.submit(wrappedTask));
    }

    /**
     * Checks if task execution is paused and waits if necessary.
     * This method is called from background tasks to implement the pause functionality.
     * It sleeps the current thread in a loop while the pause flag is set.
     */
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

    /**
     * Handles the play button click event.
     * This method resumes execution of paused tasks by clearing the pause flag.
     * It updates the UI to reflect the resumed state.
     */
    @FXML private void onPlayButtonClick() {
        pauseRequested.set(false);
        playButton.setDisable(true);
        pauseButton.setDisable(false);
        uiStateModel.setStatusMessage(I18nUtil.getMessage("status.execution.resumed"));
    }

    /**
     * Handles the pause button click event.
     * This method pauses execution of running tasks by setting the pause flag.
     * It updates the UI to reflect the paused state.
     */
    @FXML private void onPauseButtonClick() {
        pauseRequested.set(true);
        pauseButton.setDisable(true);
        playButton.setDisable(false);
        uiStateModel.setStatusMessage(I18nUtil.getMessage("status.execution.paused"));
    }

    /**
     * Handles the stop button click event.
     * This method cancels all running tasks and clears the task list.
     * It updates the UI to reflect the stopped state.
     */
    @FXML private void onStopButtonClick() {
        currentTasks.forEach(task -> task.cancel(true));
        currentTasks.clear();
        pauseRequested.set(false);
        setUiBusy(false);
        uiStateModel.setStatusMessage(I18nUtil.getMessage("status.execution.stopped"));
    }

    /**
     * Shuts down the application.
     * This method stops all running tasks, shuts down the executor service,
     * and exits the JavaFX platform.
     * It is called when the application is closing.
     */
    public void shutdown() {
        onStopButtonClick();
        executorService.shutdownNow();
        Platform.exit();
    }
    //</editor-fold>

    //<editor-fold desc="Dialogs">
    /**
     * Shows an error dialog with the specified title and message.
     * This method creates and displays an error alert dialog.
     * It is called when an error occurs during application execution.
     *
     * @param titleKey The key for the dialog title in the resource bundle
     * @param message The error message to display
     */
    private void showError(String titleKey, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18nUtil.getMessage(titleKey));
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Shows an information dialog with the specified title and message.
     * This method creates and displays an information alert dialog.
     * It is called to provide information to the user.
     *
     * @param titleKey The key for the dialog title in the resource bundle
     * @param messageKey The key for the dialog message in the resource bundle
     */
    private void showInfo(String titleKey, String messageKey) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18nUtil.getMessage(titleKey));
            alert.setHeaderText(null);
            alert.setContentText(I18nUtil.getMessage(messageKey));
            alert.showAndWait();
        });
    }

    /**
     * Shows a warning dialog with the specified title and message.
     * This method creates and displays a warning alert dialog.
     * It is called to warn the user about potential issues.
     *
     * @param titleKey The key for the dialog title in the resource bundle
     * @param messageKey The key for the dialog message in the resource bundle
     */
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
    /**
     * Gets the UI state model.
     * This method is used by the LocaleChangeService to access the current UI state
     * for preservation during locale changes.
     *
     * @return The UI state model
     */
    public UIStateModel getUiStateModel() { return uiStateModel; }

    /**
     * Gets the branch load future.
     * This method is used by the LocaleChangeService to ensure that branch loading
     * is complete before changing the locale.
     *
     * @return The CompletableFuture representing the branch loading operation
     */
    public CompletableFuture<Void> getBranchLoadFuture() { return branchLoadFuture; }
    //</editor-fold>
}
