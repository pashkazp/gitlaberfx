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
import com.depavlo.gitlaberfx.service.GitLabService;
import com.depavlo.gitlaberfx.util.DialogHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final String NOT_SELECTED_ITEM = "не обрано";

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Parses a date string in various formats to a LocalDate.
     * Tries multiple parsing strategies to handle different date formats.
     *
     * @param dateStr The date string to parse
     * @return The parsed LocalDate
     * @throws DateTimeParseException if the date string cannot be parsed
     */
    private LocalDate parseDate(String dateStr) throws DateTimeParseException {
        if (dateStr == null || dateStr.isEmpty()) {
            throw new DateTimeParseException("Date string is null or empty", dateStr, 0);
        }

        // Try multiple parsing strategies to handle different date formats
        if (dateStr.length() >= 10) {
            // First try to parse just the date part (YYYY-MM-DD)
            try {
                return LocalDate.parse(dateStr.substring(0, 10));
            } catch (DateTimeParseException e1) {
                // If that fails, try with a specific formatter for ISO format
                try {
                    DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;
                    return LocalDate.from(isoFormatter.parse(dateStr));
                } catch (DateTimeParseException e2) {
                    // Try with a custom formatter as a fallback
                    try {
                        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        return LocalDate.from(customFormatter.parse(dateStr));
                    } catch (DateTimeParseException e3) {
                        // If all parsing attempts fail, throw an exception
                        throw new DateTimeParseException("Failed to parse date after multiple attempts: " + dateStr, dateStr, 0);
                    }
                }
            }
        } else {
            // If the string is too short, try a more lenient approach
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(dateStr, formatter);
        }
    }

    // Fields to track task state
    private List<Future<?>> currentTasks = new ArrayList<>();
    private final AtomicBoolean pauseRequested = new AtomicBoolean(false);

    @FXML
    public Button mainDelMergedButton;

    @FXML
    private Button playButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button stopButton;

    @FXML
    private ComboBox<String> projectComboBox;

    @FXML
    private ComboBox<String> mainBranchComboBox;

    @FXML
    private TableView<BranchModel> branchesTableView;

    @FXML
    private TableColumn<BranchModel, Boolean> selectedColumn;

    @FXML
    private TableColumn<BranchModel, String> nameColumn;

    @FXML
    private TableColumn<BranchModel, String> lastCommitColumn;

    @FXML
    private TableColumn<BranchModel, Boolean> mergedColumn;

    @FXML
    private TableColumn<BranchModel, BranchModel> statusColumn;

    @FXML
    private Label statusLabel;

    @FXML
    private Label branchCounterLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button refreshProjectsButton;

    @FXML
    private Button refreshBranchesButton;

    @FXML
    private Button selectAllButton;

    @FXML
    private Button deselectAllButton;

    @FXML
    private Button invertSelectionButton;

    @FXML
    private Button deleteSelectedButton;

    @FXML
    private Button mainDelUnmergedButton;

    @FXML
    private Button addToExclusionsButton;

    @FXML
    private Button rescanMergedButton;

    private AppConfig config;
    private GitLabService gitLabService;
    private Stage stage;
    private String currentProjectId;

    public void initialize(AppConfig config, Stage stage) {
        this.config = config;
        this.stage = stage;

        // Налаштування колонок таблиці
        selectedColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectedColumn.setCellFactory(column -> {
            CheckBoxTableCell<BranchModel, Boolean> cell = new CheckBoxTableCell<>();
            cell.setEditable(true);
            return cell;
        });

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        lastCommitColumn.setCellValueFactory(new PropertyValueFactory<>("lastCommit"));
        mergedColumn.setCellValueFactory(new PropertyValueFactory<>("merged"));

        // Configure the status column to display icons based on branch properties
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        statusColumn.setCellFactory(column -> new TableCell<BranchModel, BranchModel>() {
            private final ImageView imageView = new ImageView();

            {
                // Configure the ImageView
                imageView.setFitHeight(16);
                imageView.setFitWidth(16);
                setGraphic(imageView);
            }

            @Override
            protected void updateItem(BranchModel branch, boolean empty) {
                super.updateItem(branch, empty);

                if (empty || branch == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    // Create a tooltip with all branch statuses
                    StringBuilder tooltipText = new StringBuilder();

                    if (branch.isMerged()) {
                        tooltipText.append("Merged\n");
                    } else {
                        tooltipText.append("Not merged\n");
                    }

                    if (branch.isProtected()) {
                        tooltipText.append("Protected\n");
                    } else {
                        tooltipText.append("Not protected\n");
                    }

                    if (branch.isDevelopersCanPush()) {
                        tooltipText.append("Developers can push\n");
                    } else {
                        tooltipText.append("Developers cannot push\n");
                    }

                    if (branch.isDevelopersCanMerge()) {
                        tooltipText.append("Developers can merge\n");
                    } else {
                        tooltipText.append("Developers cannot merge\n");
                    }

                    if (branch.isCanPush()) {
                        tooltipText.append("Can push\n");
                    } else {
                        tooltipText.append("Cannot push\n");
                    }

                    if (branch.isDefault()) {
                        tooltipText.append("Default branch");
                    } else {
                        tooltipText.append("Not default branch");
                    }

                    setTooltip(new Tooltip(tooltipText.toString()));

                    // Use Unicode characters as icons
                    HBox iconsContainer = new HBox(2); // 2 pixels spacing between icons

                    if (branch.isMerged()) {
                        Label mergedIcon = createIconLabel("✓", "Merged");
                        iconsContainer.getChildren().add(mergedIcon);
                    } else {
                        Label notMergedIcon = createIconLabel("✗", "Not merged");
                        iconsContainer.getChildren().add(notMergedIcon);
                    }

                    if (branch.isProtected()) {
                        Label protectedIcon = createIconLabel("🔒", "Protected");
                        iconsContainer.getChildren().add(protectedIcon);
                    } else {
                        Label notProtectedIcon = createIconLabel("🔓", "Not protected");
                        iconsContainer.getChildren().add(notProtectedIcon);
                    }

                    if (branch.isDevelopersCanPush()) {
                        Label devPushIcon = createIconLabel("\uD83D\uDD92", "Developers can push");
                        iconsContainer.getChildren().add(devPushIcon);
                    } else {
                        Label devPushIcon = createIconLabel("\uD83D\uDD93", "Developers can't push");
                        iconsContainer.getChildren().add(devPushIcon);
                    }

                    if (branch.isDevelopersCanMerge()) {
                        Label devMergeIcon = createIconLabel("🔄", "Developers can merge");
                        iconsContainer.getChildren().add(devMergeIcon);
                    } else {
                        Label devMergeIcon = createIconLabel("\uD83D\uDD95", "Developers can merge");
                        iconsContainer.getChildren().add(devMergeIcon);
                    }

                    if (branch.isCanPush()) {
                        Label canPushIcon = createIconLabel("➕", "Can push");
                        iconsContainer.getChildren().add(canPushIcon);
                    } else {
                        Label canPushIcon = createIconLabel("-", "Can't push");
                        iconsContainer.getChildren().add(canPushIcon);
                    }

                    if (branch.isDefault()) {
                        Label defaultIcon = createIconLabel("⭐", "Default branch");
                        iconsContainer.getChildren().add(defaultIcon);
                    } else {
                        Label defaultIcon = createIconLabel(" ", "Default branch");
                        iconsContainer.getChildren().add(defaultIcon);
                    }

                    setText(null);
                    setGraphic(iconsContainer);
                }
            }

            private Label createIconLabel(String iconText, String tooltipText) {
                Label iconLabel = new Label(iconText);
                iconLabel.setStyle("-fx-font-size: 14px;"); // Adjust size as needed
                Tooltip tooltip = new Tooltip(tooltipText);
                Tooltip.install(iconLabel, tooltip);
                return iconLabel;
            }
        });

        // Initialize mainBranchComboBox with "not selected" item
        List<String> initialItems = new ArrayList<>();
        initialItems.add(NOT_SELECTED_ITEM);
        mainBranchComboBox.setItems(FXCollections.observableArrayList(initialItems));
        mainBranchComboBox.setValue(NOT_SELECTED_ITEM);

        // Налаштування комбобоксів
        projectComboBox.setOnAction(e -> onProjectSelected());
        mainBranchComboBox.setOnAction(e -> onMainBranchSelected());

        // Налаштування TableView для редагування
        branchesTableView.setEditable(true);

        // Додавання обробника клавіш для перемикання чекбоксів пробілом
        branchesTableView.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.SPACE) {
                BranchModel selectedBranch = branchesTableView.getSelectionModel().getSelectedItem();
                if (selectedBranch != null) {
                    selectedBranch.setSelected(!selectedBranch.isSelected());
                    updateBranchCounter();
                    event.consume();
                }
            }
        });

        // Initialize branch counter
        updateBranchCounter();

        // Initialize control buttons
        playButton.setTooltip(new Tooltip("Продовжити виконання"));
        pauseButton.setTooltip(new Tooltip("Призупинити виконання"));
        stopButton.setTooltip(new Tooltip("Зупинити виконання"));

        // Initially disable control buttons
        playButton.setDisable(true);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);

        // Initially disable rescan button until a main branch is selected
        rescanMergedButton.setDisable(true);
        rescanMergedButton.setTooltip(new Tooltip("Пересканувати злиті гілки"));

        // Initialize progress bar
        progressBar.setProgress(0.0);

        // Завантаження налаштувань
        loadConfig();
    }

    private void loadConfig() {
        gitLabService = new GitLabService(config);

        // Check if required configuration is present
        if (!gitLabService.hasRequiredConfig()) {
            logger.warn("Missing required GitLab configuration");
            showWarning("Відсутні налаштування", "Відсутні необхідні налаштування для з'єднання з GitLab. Будь ласка, перевірте URL GitLab та API ключ у налаштуваннях.");
            return;
        }

        try {
            gitLabService.connect();

//            List<GitLabService.Project> projects = gitLabService.getProjects();
//            List<String> projectNames = new ArrayList<>();
//            projectNames.add(NOT_SELECTED_ITEM);
//            projectNames.addAll(projects.stream()
//                    .map(GitLabService.Project::getPathName)
//                    .sorted(String.CASE_INSENSITIVE_ORDER)
//                    .collect(Collectors.toList()));
//            projectComboBox.setItems(FXCollections.observableArrayList(projectNames));
//            projectComboBox.setValue(NOT_SELECTED_ITEM);
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            showError("Помилка завантаження", "Не вдалося завантажити налаштування: " + e.getMessage());
        }
    }

    private void onProjectSelected() {
        String projectName = projectComboBox.getValue();
        // Save current main branch selection before updating
        String currentMainBranch = mainBranchComboBox.getValue();

        // Clear mainBranchComboBox when a project is selected
        mainBranchComboBox.getItems().clear();

        // Always add "not selected" item as the first option
        List<String> branchNames = new ArrayList<>();
        branchNames.add(NOT_SELECTED_ITEM);
        mainBranchComboBox.setItems(FXCollections.observableArrayList(branchNames));
        mainBranchComboBox.setValue(NOT_SELECTED_ITEM);

        // If "not selected" is chosen, clear the branch list and return
        if (projectName == null || NOT_SELECTED_ITEM.equals(projectName)) {
            branchesTableView.setItems(FXCollections.observableArrayList());
            updateStatus("Готово");
            updateProgress(0.0);
            updateBranchCounter();
            return;
        }

        if (projectName != null) {
            // Update status bar
            updateStatus("Завантаження гілок проєкту...");

            submitTask(() -> {
                try {
                    // Виконання довготривалих операцій у фоновому потоці
                    List<GitLabService.Project> projects = null;
                    try {
                        projects = gitLabService.getProjects();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // Extract project by matching subgroup/name format
                    GitLabService.Project selectedProject = projects.stream()
                            .filter(p -> p.getPathName().equals(projectName))
                            .findFirst()
                            .orElse(null);

                    if (selectedProject != null) {
                        String projectId = String.valueOf(selectedProject.getId());

                        List<BranchModel> branches = gitLabService.getBranches(projectId);
                        // Сортування гілок за назвою (не чутливо до регістру)
                        branches.sort((b1, b2) -> String.CASE_INSENSITIVE_ORDER.compare(b1.getName(), b2.getName()));

                        // Create a copy of branchNames for thread safety
                        List<String> updatedBranchNames = new ArrayList<>(branchNames);
                        updatedBranchNames.addAll(
                                branches.stream()
                                        .map(BranchModel::getName)
                                        .sorted(String.CASE_INSENSITIVE_ORDER)
                                        .collect(Collectors.toList())
                        );

                        // Оновлення UI в потоці JavaFX
                        Platform.runLater(() -> {
                            currentProjectId = projectId;
                            config.save();

                            ObservableList<BranchModel> branchItems = FXCollections.observableArrayList(branches);
                            branchesTableView.setItems(branchItems);

                            // Add listeners to branch selection changes
                            addBranchSelectionListeners(branchItems);

                            mainBranchComboBox.setItems(FXCollections.observableArrayList(updatedBranchNames));

                            // Restore the previously selected main branch if it still exists in the updated list
                            if (currentMainBranch != null && updatedBranchNames.contains(currentMainBranch)) {
                                mainBranchComboBox.setValue(currentMainBranch);
                            } else {
                                mainBranchComboBox.setValue(NOT_SELECTED_ITEM);
                            }

                            updateStatus("Готово");
                            updateBranchCounter();
                        });
                    } else {
                        Platform.runLater(() -> {
                            updateStatus("Готово");
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        logger.error("Error loading project branches", e);
                        showError("Помилка завантаження", "Не вдалося завантажити гілки: " + e.getMessage());
                        updateStatus("Помилка завантаження");
                    });
                }
            });
        }
    }

    private void onMainBranchSelected() {
        String mainBranch = mainBranchComboBox.getValue();
        if (mainBranch != null) {
            // Set the initial state of the rescan button based on whether a main branch is selected
            rescanMergedButton.setDisable(NOT_SELECTED_ITEM.equals(mainBranch));

            ObservableList<BranchModel> branches = branchesTableView.getItems();
            if (branches != null) {
                // If "not selected" item is selected, reset the "Merged" flag for all branches
                if (NOT_SELECTED_ITEM.equals(mainBranch)) {
                    for (BranchModel branch : branches) {
                        branch.setMerged(false);
                    }
                    updateBranchCounter();
                } else {
                    // Update status bar
                    updateStatus("Перевірка злиття гілок...");
                    updateProgress(0.0);

                    // Create a copy of the branches list for thread safety
                    List<BranchModel> branchesCopy = new ArrayList<>(branches);
                    String finalMainBranch = mainBranch;
                    final int totalBranches = branchesCopy.size();

                    submitTask(() -> {
                        try {
                            // Check if branches have been merged into the selected main branch
                            int branchCounter = 0;
                            outerLoop: for (BranchModel branch : branchesCopy) {
                                // Update progress
                                final double progress = (double) branchCounter / totalBranches;
                                updateProgress(progress);
                                // Check if pause is requested
                                while (pauseRequested.get()) {
                                    // Sleep while paused
                                    try {
                                        Thread.sleep(100);
                                        // Check if thread was interrupted while sleeping
                                        if (Thread.currentThread().isInterrupted()) {
                                            break outerLoop;
                                        }
                                    } catch (InterruptedException e) {
                                        // Restore interrupt status and exit
                                        Thread.currentThread().interrupt();
                                        break outerLoop;
                                    }
                                }

                                // If thread was interrupted, exit the loop
                                if (Thread.currentThread().isInterrupted()) {
                                    break outerLoop;
                                }

                                try {
                                    // Skip checking the main branch itself
                                    if (branch.getName().equals(finalMainBranch)) {
                                        Platform.runLater(() -> branch.setMerged(false));
                                        continue outerLoop;
                                    }
                                    updateStatus("Перевірка гілки: " + branch.getName());
                                    boolean isMerged = gitLabService.isCommitInMainBranch(currentProjectId, branch.getName(), finalMainBranch);

                                    // Update UI in JavaFX thread
                                    final boolean finalIsMerged = isMerged;
                                    Platform.runLater(() -> branch.setMerged(finalIsMerged));
                                } catch (IOException e) {
                                    logger.error("Error checking if branch {} is merged into {}", branch.getName(), finalMainBranch, e);
                                    Platform.runLater(() -> branch.setMerged(false));
                                }

                                // Increment branch counter
                                branchCounter++;
                            }

                            // Update status bar and progress bar in JavaFX thread
                            Platform.runLater(() -> {
                                // Set progress to 1.0 to indicate completion
                                progressBar.setProgress(1.0);
                                // Update status after a short delay to show the completed progress
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(500);
                                        Platform.runLater(() -> {
                                            updateStatus("Готово");
                                            updateBranchCounter();
                                        });
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }).start();
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                logger.error("Error checking branch merges", e);
                                showError("Помилка перевірки", "Не вдалося перевірити злиття гілок: " + e.getMessage());
                                // Directly set progress to 0.0 to avoid conflict with updateStatus
                                progressBar.setProgress(0.0);
                                updateStatus("Помилка перевірки");
                            });
                        }
                    });
                }
            }
        }
    }

    @FXML
    private void showSettings() {
        logger.debug("Showing settings dialog");
        if (DialogHelper.showSettingsDialog(stage, config)) {
            loadConfig();
        }
    }

    @FXML
    private void exit() {
        shutdown();
    }

    /**
     * Shuts down the application, canceling all running tasks and cleaning up resources.
     * This method can be called from outside the controller to ensure proper cleanup.
     */
    public void shutdown() {
        logger.info("Exiting application");

        // Shutdown the executor service and cancel tasks
        shutdownExecutor();

        // Exit the JavaFX Platform
        Platform.exit();
    }

    /**
     * Shuts down the executor service and cancels all running tasks.
     * This method can be called separately from shutdown() to clean up resources
     * without exiting the application, especially during abnormal termination.
     */
    public void shutdownExecutor() {
        logger.info("Shutting down executor service");

        // Cancel all running tasks
        for (Future<?> task : currentTasks) {
            if (task != null && !task.isDone()) {
                // Cancel the task with interruption
                task.cancel(true);
            }
        }

        // Clear the tasks list
        currentTasks.clear();

        // Shutdown the executor service to prevent resource leaks
        executorService.shutdownNow();

        logger.info("Executor service shutdown complete");
    }

    @FXML
    private void showAbout() {
        logger.debug("Showing about dialog");
        DialogHelper.showAboutDialog(stage);
    }

    @FXML
    private void refreshBranches() {
        logger.debug("Refreshing branches list");
        // onProjectSelected() already shows and hides the loading dialog
        onProjectSelected();
    }

    @FXML
    public void refreshProjects() {
        logger.debug("Refreshing projects list from GitLab");

        // Save current project and main branch selection before updating
        String currentProject = projectComboBox.getValue();
        String currentMainBranch = mainBranchComboBox.getValue();

        // Update status bar
        updateStatus("Оновлення списку проєктів з GitLab...");

        submitTask(() -> {
            try {
                // Get projects from GitLab
                List<GitLabService.Project> projects = gitLabService.getProjects();

                // Create a list of project names
                List<String> projectNames = new ArrayList<>();
                projectNames.add(NOT_SELECTED_ITEM);
                projectNames.addAll(projects.stream()
                        .map(GitLabService.Project::getPathName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList()));

                // Update UI in JavaFX thread
                Platform.runLater(() -> {
                    // Update the project combobox
                    projectComboBox.setItems(FXCollections.observableArrayList(projectNames));

                    // Check if the current project still exists in the updated list
                    if (currentProject != null && projectNames.contains(currentProject)) {
                        // Restore the current project
                        projectComboBox.setValue(currentProject);

                        // Get the branches for the current project
                        updateStatus("Оновлення гілок проєкту...");

                        // The onProjectSelected() method will be called automatically when the project is selected,
                        // which will update the branches and restore the main branch if it still exists
                    } else {
                        // Reset both project and main branch to "not selected"
                        projectComboBox.setValue(NOT_SELECTED_ITEM);
                        mainBranchComboBox.setValue(NOT_SELECTED_ITEM);

                        // Clear the branches table
                        branchesTableView.setItems(FXCollections.observableArrayList());

                        updateStatus("Готово");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("Error refreshing projects", e);
                    showError("Помилка оновлення", "Не вдалося оновити список проєктів: " + e.getMessage());
                    updateStatus("Помилка оновлення");
                });
            }
        });
    }

    @FXML
    private void selectAll() {
        logger.debug("Selecting all branches");
        branchesTableView.getItems().forEach(branch -> branch.setSelected(true));
        updateBranchCounter();
    }

    @FXML
    private void deselectAll() {
        logger.debug("Deselecting all branches");
        branchesTableView.getItems().forEach(branch -> branch.setSelected(false));
        updateBranchCounter();
    }

    @FXML
    private void invertSelection() {
        logger.debug("Inverting selection");
        branchesTableView.getItems().forEach(branch -> branch.setSelected(!branch.isSelected()));
        updateBranchCounter();
    }

    @FXML
    private void deleteSelected() {
        logger.debug("Deleting selected branches");
        List<BranchModel> selectedBranches = branchesTableView.getItems().stream()
                .filter(BranchModel::isSelected)
                .collect(Collectors.toList());

        if (!selectedBranches.isEmpty()) {
            List<BranchModel> confirmedBranches = DialogHelper.showDeleteConfirmationDialog(stage, selectedBranches);
            if (confirmedBranches != null && !confirmedBranches.isEmpty()) {
                // Update status bar and initialize progress bar
                updateStatus("Видалення вибраних гілок...");
                updateProgress(0.0);

                // Create a copy of the confirmed branches list for thread safety
                List<BranchModel> branchesToDelete = new ArrayList<>(confirmedBranches);
                final int totalBranches = branchesToDelete.size();

                submitTask(() -> {
                    try {
                        int branchCounter = 0;
                        outerLoop: for (BranchModel branch : branchesToDelete) {
                            // Update progress
                            final double progress = (double) branchCounter / totalBranches;
                            updateProgress(progress);

                            // Check if pause is requested
                            while (pauseRequested.get()) {
                                // Sleep while paused
                                try {
                                    Thread.sleep(100);
                                    // Check if thread was interrupted while sleeping
                                    if (Thread.currentThread().isInterrupted()) {
                                        break outerLoop;
                                    }
                                } catch (InterruptedException e) {
                                    // Restore interrupt status and exit
                                    Thread.currentThread().interrupt();
                                    break outerLoop;
                                }
                            }

                            // If thread was interrupted, exit the loop
                            if (Thread.currentThread().isInterrupted()) {
                                break outerLoop;
                            }

                            updateStatus("Видалення гілки: " + branch.getName());
                            gitLabService.deleteBranch(currentProjectId, branch.getName());

                            // Increment branch counter
                            branchCounter++;
                        }

                        // Set progress to 1.0 to indicate completion
                        updateProgress(1.0);

                        // Update status bar before refreshing branches
                        Platform.runLater(() -> {
                            updateStatus("Оновлення списку гілок...");
                            // refreshBranches() will update the status bar
                            refreshBranches();
                            // updateBranchCounter will be called by onProjectSelected
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            logger.error("Error deleting branches", e);
                            // Update status bar and reset progress bar in case of error
                            updateProgress(0.0);
                            updateStatus("Помилка видалення гілок");
                            showError("Помилка видалення", "Не вдалося видалити гілки: " + e.getMessage());
                        });
                    }
                });
            }
        }
    }

    @FXML
    private void deleteMerged() {
        logger.debug("Checking merged branches");
        String mainBranch = mainBranchComboBox.getValue();
        if (mainBranch == null || NOT_SELECTED_ITEM.equals(mainBranch)) {
            showError("Помилка", "Не вибрано головну гілку");
            return;
        }

        LocalDate cutoffDate = DialogHelper.showDatePickerDialog(stage);
        if (cutoffDate != null) {
            // Update status bar
            updateStatus("Перевірка змерджених гілок...");
            updateProgress(0.0);

            // Store final values for use in lambda
            final String finalMainBranch = mainBranch;
            final LocalDate finalCutoffDate = cutoffDate;

            submitTask(() -> {
                try {
                    // Create a copy of the branches list for thread safety
                    List<BranchModel> branchesCopy = new ArrayList<>(branchesTableView.getItems());

                    // Create a list to store merged branches
                    List<BranchModel> mergedBranches = new ArrayList<>();

                    // Iterate through each branch and check if it meets the criteria
                    final int totalBranches = branchesCopy.size();
                    int branchCounter = 0;

                    outerLoop: for (BranchModel branch : branchesCopy) {
                        // Update progress
                        final double progress = (double) branchCounter / totalBranches;
                        updateProgress(progress);

                        // Check if pause is requested
                        while (pauseRequested.get()) {
                            // Sleep while paused
                            try {
                                Thread.sleep(100);
                                // Check if thread was interrupted while sleeping
                                if (Thread.currentThread().isInterrupted()) {
                                    break outerLoop;
                                }
                            } catch (InterruptedException e) {
                                // Restore interrupt status and exit
                                Thread.currentThread().interrupt();
                                break outerLoop;
                            }
                        }

                        // If thread was interrupted, exit the loop
                        if (Thread.currentThread().isInterrupted()) {
                            break outerLoop;
                        }

                        // Check if the branch is merged into the main branch
                        try {
                            updateStatus("Перевірка гілки: " + branch.getName());
                            boolean isMerged = gitLabService.isCommitInMainBranch(currentProjectId, branch.getName(), finalMainBranch);

                            // If the branch is not merged, skip to the next branch
                            if (!isMerged) {
                                branchCounter++;
                                continue outerLoop;
                            }
                        } catch (IOException e) {
                            logger.error("Error checking if branch is merged", e);
                            branchCounter++;
                            continue outerLoop; // Skip to the next branch if there's an error
                        }

                        // Parse the last commit date and compare it with the cutoff date
                        String lastCommitDateStr = branch.getLastCommit();
                        if (lastCommitDateStr == null || lastCommitDateStr.isEmpty()) {
                            branchCounter++;
                            continue outerLoop; // Skip to the next branch if there's no commit date
                        }

                        try {
                            // The lastCommit is in ISO 8601 format, e.g. "2023-01-01T12:00:00Z"
                            // We need to parse it to a LocalDate for comparison using our helper method
                            LocalDate lastCommitDate = parseDate(lastCommitDateStr);

                            if (lastCommitDate.isBefore(finalCutoffDate)) {
                                // If the branch meets all criteria, add it to the merged branches list
                                mergedBranches.add(branch);
                            }
                        } catch (Exception e) {
                            logger.error("Error parsing last commit date: {}", lastCommitDateStr, e);
                            // Skip to the next branch if there's an error parsing the date
                        }

                        // Increment branch counter
                        branchCounter++;
                    }

                    // Set progress to 1.0 to indicate completion of checking phase
                    updateProgress(1.0);

                    // Update UI in JavaFX thread
                    Platform.runLater(() -> {
                        // Update status bar before showing confirmation dialog
                        updateStatus("Готово");

                        if (!mergedBranches.isEmpty()) {
                            List<BranchModel> confirmedBranches = DialogHelper.showDeleteConfirmationDialog(stage, mergedBranches);
                            if (confirmedBranches != null && !confirmedBranches.isEmpty()) {
                                // Update status bar for deletion
                                updateStatus("Видалення змерджених гілок...");
                                updateProgress(0.0);

                                // Create a copy of the confirmed branches list for thread safety
                                List<BranchModel> branchesToDelete = new ArrayList<>(confirmedBranches);
                                final int totalBranchesToDelete = branchesToDelete.size();

                                // Submit a new task for deletion
                                submitTask(() -> {
                                    try {
                                        int deleteCounter = 0;
                                        outerLoop: for (BranchModel branch : branchesToDelete) {
                                            // Update progress
                                            final double progress = (double) deleteCounter / totalBranchesToDelete;
                                            updateProgress(progress);

                                            // Check if pause is requested
                                            while (pauseRequested.get()) {
                                                // Sleep while paused
                                                try {
                                                    Thread.sleep(100);
                                                    // Check if thread was interrupted while sleeping
                                                    if (Thread.currentThread().isInterrupted()) {
                                                        break outerLoop;
                                                    }
                                                } catch (InterruptedException e) {
                                                    // Restore interrupt status and exit
                                                    Thread.currentThread().interrupt();
                                                    break outerLoop;
                                                }
                                            }

                                            // If thread was interrupted, exit the loop
                                            if (Thread.currentThread().isInterrupted()) {
                                                break outerLoop;
                                            }

                                            updateStatus("Видалення гілки: " + branch.getName());
                                            gitLabService.deleteBranch(currentProjectId, branch.getName());

                                            // Increment delete counter
                                            deleteCounter++;
                                        }

                                        // Set progress to 1.0 to indicate completion of deletion phase
                                        updateProgress(1.0);

                                        // Update UI in JavaFX thread
                                        Platform.runLater(() -> {
                                            // Update status bar before refreshing branches
                                            updateStatus("Оновлення списку гілок...");
                                            // refreshBranches() will update the status bar
                                            refreshBranches();
                                            // updateBranchCounter will be called by onProjectSelected
                                        });
                                    } catch (IOException e) {
                                        Platform.runLater(() -> {
                                            logger.error("Error deleting merged branches", e);
                                            // Update status bar in case of error
                                            updateStatus("Помилка видалення гілок");
                                            updateProgress(0.0);
                                            showError("Помилка видалення", "Не вдалося видалити гілки: " + e.getMessage());
                                            updateBranchCounter();
                                        });
                                    }
                                });
                            }
                        } else {
                            updateStatus("Готово");
                            showInfo("Інформація", "Не знайдено змерджених гілок, які старіші за вказану дату");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        logger.error("Error checking merged branches", e);
                        // Update status bar in case of error
                        updateStatus("Помилка перевірки гілок");
                        updateProgress(0.0);
                        showError("Помилка", "Не вдалося перевірити гілки: " + e.getMessage());
                    });
                }
            });
        }
    }

    @FXML
    private void deleteUnmerged() {
        logger.debug("Checking unmerged branches");
        String mainBranch = mainBranchComboBox.getValue();
        if (mainBranch == null || NOT_SELECTED_ITEM.equals(mainBranch)) {
            showError("Помилка", "Не вибрано головну гілку");
            return;
        }

        LocalDate cutoffDate = DialogHelper.showDatePickerDialog(stage);
        if (cutoffDate != null) {
            // Update status bar
            updateStatus("Перевірка не змерджених гілок...");
            updateProgress(0.0);

            // Store final values for use in lambda
            final String finalMainBranch = mainBranch;
            final LocalDate finalCutoffDate = cutoffDate;

            submitTask(() -> {
                try {
                    // Create a copy of the branches list for thread safety
                    List<BranchModel> branchesCopy = new ArrayList<>(branchesTableView.getItems());

                    // Create a list to store unmerged branches
                    List<BranchModel> unmergedBranches = new ArrayList<>();

                    // Iterate through each branch and check if it meets the criteria
                    final int totalBranches = branchesCopy.size();
                    int branchCounter = 0;

                    outerLoop: for (BranchModel branch : branchesCopy) {
                        // Update progress
                        final double progress = (double) branchCounter / totalBranches;
                        updateProgress(progress);

                        // Check if pause is requested
                        while (pauseRequested.get()) {
                            // Sleep while paused
                            try {
                                Thread.sleep(100);
                                // Check if thread was interrupted while sleeping
                                if (Thread.currentThread().isInterrupted()) {
                                    break outerLoop;
                                }
                            } catch (InterruptedException e) {
                                // Restore interrupt status and exit
                                Thread.currentThread().interrupt();
                                break outerLoop;
                            }
                        }

                        // If thread was interrupted, exit the loop
                        if (Thread.currentThread().isInterrupted()) {
                            break outerLoop;
                        }

                        // Check if the branch is merged into the main branch
                        try {
                            updateStatus("Перевірка гілки: " + branch.getName());
                            boolean isMerged = gitLabService.isCommitInMainBranch(currentProjectId, branch.getName(), finalMainBranch);

                            // If the branch is merged, skip to the next branch (inverse of deleteMerged logic)
                            if (isMerged) {
                                branchCounter++;
                                continue outerLoop;
                            }
                        } catch (IOException e) {
                            logger.error("Error checking if branch is merged", e);
                            branchCounter++;
                            continue outerLoop; // Skip to the next branch if there's an error
                        }

                        // Parse the last commit date and compare it with the cutoff date
                        String lastCommitDateStr = branch.getLastCommit();
                        if (lastCommitDateStr == null || lastCommitDateStr.isEmpty()) {
                            branchCounter++;
                            continue outerLoop; // Skip to the next branch if there's no commit date
                        }

                        try {
                            // The lastCommit is in ISO 8601 format, e.g. "2023-01-01T12:00:00Z"
                            // We need to parse it to a LocalDate for comparison using our helper method
                            LocalDate lastCommitDate = parseDate(lastCommitDateStr);

                            if (lastCommitDate.isBefore(finalCutoffDate)) {
                                // If the branch meets all criteria, add it to the unmerged branches list
                                unmergedBranches.add(branch);
                            }
                        } catch (Exception e) {
                            logger.error("Error parsing last commit date: {}", lastCommitDateStr, e);
                            // Skip to the next branch if there's an error parsing the date
                        }

                        // Increment branch counter
                        branchCounter++;
                    }

                    // Set progress to 1.0 to indicate completion of checking phase
                    updateProgress(1.0);

                    // Update UI in JavaFX thread
                    Platform.runLater(() -> {
                        // Update status bar before showing confirmation dialog
                        updateStatus("Готово");

                        if (!unmergedBranches.isEmpty()) {
                            List<BranchModel> confirmedBranches = DialogHelper.showDeleteConfirmationDialog(stage, unmergedBranches);
                            if (confirmedBranches != null && !confirmedBranches.isEmpty()) {
                                // Update status bar for deletion
                                updateStatus("Видалення не змерджених гілок...");
                                updateProgress(0.0);

                                // Create a copy of the confirmed branches list for thread safety
                                List<BranchModel> branchesToDelete = new ArrayList<>(confirmedBranches);
                                final int totalBranchesToDelete = branchesToDelete.size();

                                // Submit a new task for deletion
                                submitTask(() -> {
                                    try {
                                        int deleteCounter = 0;
                                        outerLoop: for (BranchModel branch : branchesToDelete) {
                                            // Update progress
                                            final double progress = (double) deleteCounter / totalBranchesToDelete;
                                            updateProgress(progress);

                                            // Check if pause is requested
                                            while (pauseRequested.get()) {
                                                // Sleep while paused
                                                try {
                                                    Thread.sleep(100);
                                                    // Check if thread was interrupted while sleeping
                                                    if (Thread.currentThread().isInterrupted()) {
                                                        break outerLoop;
                                                    }
                                                } catch (InterruptedException e) {
                                                    // Restore interrupt status and exit
                                                    Thread.currentThread().interrupt();
                                                    break outerLoop;
                                                }
                                            }

                                            // If thread was interrupted, exit the loop
                                            if (Thread.currentThread().isInterrupted()) {
                                                break outerLoop;
                                            }

                                            updateStatus("Видалення гілки: " + branch.getName());
                                            gitLabService.deleteBranch(currentProjectId, branch.getName());

                                            // Increment delete counter
                                            deleteCounter++;
                                        }

                                        // Set progress to 1.0 to indicate completion of deletion phase
                                        updateProgress(1.0);

                                        // Update UI in JavaFX thread
                                        Platform.runLater(() -> {
                                            // Update status bar before refreshing branches
                                            updateStatus("Оновлення списку гілок...");
                                            // refreshBranches() will update the status bar
                                            refreshBranches();
                                            // updateBranchCounter will be called by onProjectSelected
                                        });
                                    } catch (IOException e) {
                                        Platform.runLater(() -> {
                                            logger.error("Error deleting unmerged branches", e);
                                            // Update status bar in case of error
                                            updateStatus("Помилка видалення гілок");
                                            updateProgress(0.0);
                                            showError("Помилка видалення", "Не вдалося видалити гілки: " + e.getMessage());
                                            updateBranchCounter();
                                        });
                                    }
                                });
                            }
                        } else {
                            updateStatus("Готово");
                            showInfo("Інформація", "Не знайдено не змерджених гілок, які старіші за вказану дату");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        logger.error("Error checking unmerged branches", e);
                        // Update status bar in case of error
                        updateStatus("Помилка перевірки гілок");
                        updateProgress(0.0);
                        showError("Помилка", "Не вдалося перевірити гілки: " + e.getMessage());
                    });
                }
            });
        }
    }

    @FXML
    private void addToExclusions() {
        logger.debug("Adding to exclusions");
        List<BranchModel> selectedBranches = branchesTableView.getItems().stream()
                .filter(BranchModel::isSelected)
                .collect(Collectors.toList());

        if (!selectedBranches.isEmpty()) {
            config.getExcludedBranches().addAll(
                    selectedBranches.stream()
                            .map(BranchModel::getName)
                            .collect(Collectors.toList())
            );
            config.save();
            showInfo("Інформація", "Гілки додано до виключень");
        }
    }

    @FXML
    private void rescanMerged() {
        logger.debug("Rescanning merged branches");
        String mainBranch = mainBranchComboBox.getValue();

        // Check if a main branch is selected
        if (mainBranch == null || NOT_SELECTED_ITEM.equals(mainBranch)) {
            showError("Помилка", "Не вибрано головну гілку");
            return;
        }

        ObservableList<BranchModel> branches = branchesTableView.getItems();
        if (branches == null || branches.isEmpty()) {
            showInfo("Інформація", "Немає гілок для перевірки");
            return;
        }

        // Update status bar
        updateStatus("Перевірка злиття гілок...");

        // Create a copy of the branches list for thread safety
        List<BranchModel> branchesCopy = new ArrayList<>(branches);
        String finalMainBranch = mainBranch;

        submitTask(() -> {
            try {
                // Check if branches have been merged into the selected main branch
                outerLoop: for (BranchModel branch : branchesCopy) {
                    // Check if pause is requested
                    while (pauseRequested.get()) {
                        // Sleep while paused
                        try {
                            Thread.sleep(100);
                            // Check if thread was interrupted while sleeping
                            if (Thread.currentThread().isInterrupted()) {
                                break outerLoop;
                            }
                        } catch (InterruptedException e) {
                            // Restore interrupt status and exit
                            Thread.currentThread().interrupt();
                            break outerLoop;
                        }
                    }

                    // If thread was interrupted, exit the loop
                    if (Thread.currentThread().isInterrupted()) {
                        break outerLoop;
                    }

                    try {
                        // Skip checking the main branch itself
                        if (branch.getName().equals(finalMainBranch)) {
                            Platform.runLater(() -> branch.setMerged(false));
                            continue outerLoop;
                        }
                        updateStatus("Перевірка гілки: " + branch.getName());
                        boolean isMerged = gitLabService.isCommitInMainBranch(currentProjectId, branch.getName(), finalMainBranch);

                        // Update UI in JavaFX thread
                        final boolean finalIsMerged = isMerged;
                        Platform.runLater(() -> branch.setMerged(finalIsMerged));
                    } catch (IOException e) {
                        logger.error("Error checking if branch {} is merged into {}", branch.getName(), finalMainBranch, e);
                        Platform.runLater(() -> branch.setMerged(false));
                    }
                }

                // Update status bar in JavaFX thread
                Platform.runLater(() -> updateStatus("Готово"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("Error checking branch merges", e);
                    showError("Помилка перевірки", "Не вдалося перевірити злиття гілок: " + e.getMessage());
                    updateStatus("Помилка перевірки");
                });
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Updates the status label with the given message.
     * This method is safe to call from any thread.
     * If the message is "Готово", it also resets the progress bar to 0.0.
     * 
     * @param message The message to display
     */
    private void updateStatus(String message) {
        if (Platform.isFxApplicationThread()) {
            statusLabel.setText(message);
            // Reset progress bar when status is "Готово" (Ready)
            if ("Готово".equals(message)) {
                progressBar.setProgress(0.0);
            }
        } else {
            Platform.runLater(() -> {
                statusLabel.setText(message);
                // Reset progress bar when status is "Готово" (Ready)
                if ("Готово".equals(message)) {
                    progressBar.setProgress(0.0);
                }
            });
        }
    }

    /**
     * Updates the progress bar with the given progress value.
     * This method is safe to call from any thread.
     * 
     * @param progress The progress value between 0.0 and 1.0
     */
    private void updateProgress(double progress) {
        if (Platform.isFxApplicationThread()) {
            progressBar.setProgress(progress);
        } else {
            Platform.runLater(() -> progressBar.setProgress(progress));
        }
    }

    /**
     * Updates the branch counter label with the current count of selected branches and total branches.
     * This method is safe to call from any thread.
     */
    private void updateBranchCounter() {
        int totalBranches = branchesTableView.getItems().size();
        int selectedBranches = (int) branchesTableView.getItems().stream()
                .filter(BranchModel::isSelected)
                .count();

        if (Platform.isFxApplicationThread()) {
            branchCounterLabel.setText(selectedBranches + "/" + totalBranches);
        } else {
            Platform.runLater(() -> branchCounterLabel.setText(selectedBranches + "/" + totalBranches));
        }
    }

    /**
     * Adds listeners to each branch's selectedProperty to update the counter when selection changes.
     * This ensures the counter is updated when branches are selected/deselected with the mouse.
     * 
     * @param branches The list of branches to add listeners to
     */
    private void addBranchSelectionListeners(List<BranchModel> branches) {
        if (branches == null) return;

        for (BranchModel branch : branches) {
            // Add listener to the selectedProperty
            branch.selectedProperty().addListener((observable, oldValue, newValue) -> {
                // Update the counter when the selection changes
                updateBranchCounter();
            });
        }
    }

    /**
     * Handles the play button click event.
     * Resumes any paused tasks.
     */
    @FXML
    private void onPlayButtonClick() {
        logger.debug("Play button clicked");
        pauseRequested.set(false);
        playButton.setDisable(true);
        pauseButton.setDisable(false);
        updateStatus("Виконання відновлено");
    }

    /**
     * Handles the pause button click event.
     * Pauses any running tasks.
     */
    @FXML
    private void onPauseButtonClick() {
        logger.debug("Pause button clicked");
        pauseRequested.set(true);
        pauseButton.setDisable(true);
        playButton.setDisable(false);
        updateStatus("Виконання призупинено");
    }

    /**
     * Handles the stop button click event.
     * Stops any running tasks non-destructively.
     */
    @FXML
    private void onStopButtonClick() {
        logger.debug("Stop button clicked");
        boolean tasksRunning = false;

        // Cancel all running tasks
        for (Future<?> task : currentTasks) {
            if (task != null && !task.isDone()) {
                // Cancel the task with interruption
                task.cancel(true);
                tasksRunning = true;
            }
        }

        if (tasksRunning) {
            pauseRequested.set(false);
            updateStatus("Виконання зупинено");

            // Disable control buttons
            playButton.setDisable(true);
            pauseButton.setDisable(true);
            stopButton.setDisable(true);

            // Re-enable UI elements when tasks are stopped
            setUiElementsDisabled(false);

            // Clear the tasks list
            currentTasks.clear();
        }
    }

    /**
     * Submits a task to the ExecutorService with support for pausing and stopping.
     * Note: The actual pause handling is implemented in the tasks themselves.
     * 
     * @param task The task to submit
     * @return The Future representing the submitted task
     */
    private Future<?> submitTask(Runnable task) {
        return submitTasks(List.of(task));
    }

    /**
     * Enables or disables UI elements during background operations.
     * 
     * @param disable true to disable UI elements, false to enable them
     */
    private void setUiElementsDisabled(boolean disable) {
        Platform.runLater(() -> {
            // Disable/enable buttons
            refreshProjectsButton.setDisable(disable);
            refreshBranchesButton.setDisable(disable);
            selectAllButton.setDisable(disable);
            deselectAllButton.setDisable(disable);
            invertSelectionButton.setDisable(disable);
            deleteSelectedButton.setDisable(disable);
            mainDelMergedButton.setDisable(disable);
            mainDelUnmergedButton.setDisable(disable);
            addToExclusionsButton.setDisable(disable);

            // Disable the rescan button during background operations
            // but only if a main branch is selected (otherwise it should remain disabled)
            if (disable) {
                rescanMergedButton.setDisable(true);
            } else {
                // Re-enable only if a main branch is selected
                String mainBranch = mainBranchComboBox.getValue();
                rescanMergedButton.setDisable(mainBranch == null || NOT_SELECTED_ITEM.equals(mainBranch));
            }

            // Disable/enable comboboxes
            projectComboBox.setDisable(disable);
            mainBranchComboBox.setDisable(disable);
        });
    }

    /**
     * Submits a list of tasks to the ExecutorService with support for pausing and stopping.
     * Tasks are executed sequentially in the order they are provided.
     * Note: The actual pause handling is implemented in the tasks themselves.
     * 
     * @param tasks The list of tasks to submit
     * @return The Future representing the submitted tasks
     */
    private Future<?> submitTasks(List<Runnable> tasks) {
        // Reset pause flag
        pauseRequested.set(false);

        // Enable control buttons and disable UI elements
        Platform.runLater(() -> {
            playButton.setDisable(true);  // Initially disable play button
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
        });

        // Disable UI elements during background operations
        setUiElementsDisabled(true);

        // Wrap the tasks with interrupt handling
        Runnable wrappedTask = () -> {
            try {
                // Run each task in sequence
                for (Runnable task : tasks) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    try {
                        // Run the actual task
                        task.run();
                    } catch (Exception e) {
                        logger.error("Task execution error", e);
                        Platform.runLater(() -> {
                            updateStatus("Помилка виконання");
                            showError("Помилка", "Помилка виконання: " + e.getMessage());
                        });
                        // Continue with the next task even if this one fails
                    }
                }
            } finally {
                // Disable control buttons when all tasks are done
                Platform.runLater(() -> {
                    playButton.setDisable(true);
                    pauseButton.setDisable(true);
                    stopButton.setDisable(true);
                });

                // Re-enable UI elements when all tasks are done
                setUiElementsDisabled(false);
            }
        };

        // Submit the wrapped task
        Future<?> future = executorService.submit(wrappedTask);
        currentTasks.add(future);
        return future;
    }
}
