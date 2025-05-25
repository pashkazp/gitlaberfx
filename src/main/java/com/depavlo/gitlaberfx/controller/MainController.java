package com.depavlo.gitlaberfx.controller;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.model.BranchModel;
import com.depavlo.gitlaberfx.service.GitLabService;
import com.depavlo.gitlaberfx.util.DialogHelper;
import com.depavlo.gitlaberfx.util.TaskRunner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final String NOT_SELECTED_ITEM = "не обрано";

    @FXML
    public Button mainDelMergedButton;

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
                    event.consume();
                }
            }
        });

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

        // Run the connection and project loading in a background task
        TaskRunner.runWithProgress(
                stage,
                "Завантаження проєктів",
                "З'єднання з GitLab та завантаження проєктів...",
                progressCallback -> {
                    try {
                        progressCallback.updateProgress(0.1);
                        progressCallback.updateMessage("З'єднання з GitLab...");
                        gitLabService.connect();

                        progressCallback.updateProgress(0.3);
                        progressCallback.updateMessage("Завантаження проєктів...");
                        List<GitLabService.Project> projects = gitLabService.getProjects();

                        progressCallback.updateProgress(1.0);
                        progressCallback.updateMessage("Завантаження завершено");
                        return projects;
                    } catch (IOException e) {
                        logger.error("Error loading configuration", e);
                        throw new RuntimeException(e);
                    }
                },
                projects -> {
                    // Update UI with the loaded projects
                    projectComboBox.setItems(FXCollections.observableArrayList(
                            projects.stream()
                                    .map(GitLabService.Project::getName)
                                    .sorted(String.CASE_INSENSITIVE_ORDER)
                                    .collect(Collectors.toList())
                    ));
                }
        );
    }

    private void onProjectSelected() {
        String projectName = projectComboBox.getValue();
        // Clear mainBranchComboBox when a project is selected
        mainBranchComboBox.getItems().clear();

        // Always add "not selected" item as the first option
        List<String> branchNames = new ArrayList<>();
        branchNames.add(NOT_SELECTED_ITEM);
        mainBranchComboBox.setItems(FXCollections.observableArrayList(branchNames));
        mainBranchComboBox.setValue(NOT_SELECTED_ITEM);

        if (projectName != null) {
            // Run the project selection and branch loading in a background task
            TaskRunner.runWithProgress(
                    stage,
                    "Завантаження гілок",
                    "Завантаження гілок проєкту...",
                    progressCallback -> {
                        try {
                            progressCallback.updateProgress(0.1);
                            progressCallback.updateMessage("Отримання списку проєктів...");
                            List<GitLabService.Project> projects = gitLabService.getProjects();

                            progressCallback.updateProgress(0.3);
                            progressCallback.updateMessage("Пошук обраного проєкту...");
                            GitLabService.Project selectedProject = projects.stream()
                                    .filter(p -> p.getName().equals(projectName))
                                    .findFirst()
                                    .orElse(null);

                            if (selectedProject == null) {
                                progressCallback.updateProgress(1.0);
                                progressCallback.updateMessage("Проєкт не знайдено");
                                return null;
                            }

                            currentProjectId = String.valueOf(selectedProject.getId());
                            config.save();

                            progressCallback.updateProgress(0.6);
                            progressCallback.updateMessage("Завантаження гілок...");
                            List<BranchModel> branches = gitLabService.getBranches(currentProjectId);

                            // Сортування гілок за назвою (не чутливо до регістру)
                            progressCallback.updateProgress(0.9);
                            progressCallback.updateMessage("Сортування гілок...");
                            branches.sort((b1, b2) -> String.CASE_INSENSITIVE_ORDER.compare(b1.getName(), b2.getName()));

                            progressCallback.updateProgress(1.0);
                            progressCallback.updateMessage("Завантаження завершено");
                            return branches;
                        } catch (IOException e) {
                            logger.error("Error loading project branches", e);
                            throw new RuntimeException(e);
                        }
                    },
                    branches -> {
                        if (branches != null) {
                            // Update UI with the loaded branches
                            branchesTableView.setItems(FXCollections.observableArrayList(branches));

                            // Add branch names to the list that already contains "not selected" item
                            List<String> updatedBranchNames = new ArrayList<>(branchNames);
                            updatedBranchNames.addAll(
                                    branches.stream()
                                            .map(BranchModel::getName)
                                            .sorted(String.CASE_INSENSITIVE_ORDER)
                                            .collect(Collectors.toList())
                            );
                            mainBranchComboBox.setItems(FXCollections.observableArrayList(updatedBranchNames));
                            mainBranchComboBox.setValue(NOT_SELECTED_ITEM);
                        } else {
                            showError("Помилка завантаження", "Не вдалося знайти обраний проєкт");
                        }
                    }
            );
        }
    }

    private void onMainBranchSelected() {
        String mainBranch = mainBranchComboBox.getValue();
        if (mainBranch != null) {
            ObservableList<BranchModel> branches = branchesTableView.getItems();
            if (branches != null) {
                // If "not selected" item is selected, reset the "Merged" flag for all branches
                if (NOT_SELECTED_ITEM.equals(mainBranch)) {
                    for (BranchModel branch : branches) {
                        branch.setMerged(false);
                    }
                } else {
                    // Run the merge check in a background task
                    TaskRunner.runWithProgress(
                            stage,
                            "Перевірка гілок",
                            "Перевірка гілок на змердженість...",
                            progressCallback -> {
                                try {
                                    List<BranchModel> branchesCopy = new ArrayList<>(branches);
                                    int totalBranches = branchesCopy.size();
                                    int processedBranches = 0;

                                    for (BranchModel branch : branchesCopy) {
                                        // Check if cancellation was requested
                                        if (progressCallback.isCancelRequested()) {
                                            progressCallback.updateMessage("Операцію скасовано");
                                            break;
                                        }

                                        // Skip checking the main branch itself
                                        if (branch.getName().equals(mainBranch)) {
                                            branch.setMerged(false);
                                            processedBranches++;
                                            continue;
                                        }

                                        // Update progress
                                        double progress = (double) processedBranches / totalBranches;
                                        progressCallback.updateProgress(progress);
                                        progressCallback.updateMessage("Перевірка гілки: " + branch.getName());

                                        try {
                                            boolean isMerged = gitLabService.isCommitInMainBranch(currentProjectId, branch.getName(), mainBranch);
                                            branch.setMerged(isMerged);
                                        } catch (IOException e) {
                                            logger.error("Error checking if branch {} is merged into {}", branch.getName(), mainBranch, e);
                                            branch.setMerged(false);
                                        }

                                        processedBranches++;
                                    }

                                    progressCallback.updateProgress(1.0);
                                    progressCallback.updateMessage("Перевірку завершено");
                                    return branchesCopy;
                                } catch (Exception e) {
                                    logger.error("Error checking merged branches", e);
                                    throw new RuntimeException(e);
                                }
                            },
                            updatedBranches -> {
                                // Update the UI with the updated branches
                                if (updatedBranches != null) {
                                    branchesTableView.refresh();
                                }
                            }
                    );
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
        logger.info("Exiting application");
        javafx.application.Platform.exit();
    }

    @FXML
    private void showAbout() {
        logger.debug("Showing about dialog");
        DialogHelper.showAboutDialog(stage);
    }

    @FXML
    private void refreshBranches() {
        logger.debug("Refreshing branches list");
        onProjectSelected();
    }

    @FXML
    private void selectAll() {
        logger.debug("Selecting all branches");
        branchesTableView.getItems().forEach(branch -> branch.setSelected(true));
    }

    @FXML
    private void deselectAll() {
        logger.debug("Deselecting all branches");
        branchesTableView.getItems().forEach(branch -> branch.setSelected(false));
    }

    @FXML
    private void invertSelection() {
        logger.debug("Inverting selection");
        branchesTableView.getItems().forEach(branch -> branch.setSelected(!branch.isSelected()));
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
                // Run the branch deletion in a background task
                TaskRunner.runWithProgress(
                        stage,
                        "Видалення гілок",
                        "Видалення вибраних гілок...",
                        progressCallback -> {
                            try {
                                int totalBranches = confirmedBranches.size();
                                int deletedBranches = 0;

                                for (BranchModel branch : confirmedBranches) {
                                    // Check if cancellation was requested
                                    if (progressCallback.isCancelRequested()) {
                                        progressCallback.updateMessage("Операцію скасовано");
                                        break;
                                    }

                                    // Update progress
                                    double progress = (double) deletedBranches / totalBranches;
                                    progressCallback.updateProgress(progress);
                                    progressCallback.updateMessage("Видалення гілки: " + branch.getName());

                                    gitLabService.deleteBranch(currentProjectId, branch.getName());
                                    deletedBranches++;
                                }

                                progressCallback.updateProgress(1.0);
                                progressCallback.updateMessage("Видалення завершено");
                                return deletedBranches;
                            } catch (IOException e) {
                                logger.error("Error deleting branches", e);
                                throw new RuntimeException(e);
                            }
                        },
                        deletedCount -> {
                            // Refresh the branches list after deletion
                            refreshBranches();
                        }
                );
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
            final String finalMainBranch = mainBranch;
            final LocalDate finalCutoffDate = cutoffDate;

            // Run the merged branches check and deletion in a background task
            TaskRunner.runWithProgress(
                    stage,
                    "Перевірка та видалення змерджених гілок",
                    "Пошук змерджених гілок...",
                    progressCallback -> {
                        try {
                            List<BranchModel> allBranches = new ArrayList<>(branchesTableView.getItems());
                            int totalBranches = allBranches.size();
                            int processedBranches = 0;
                            List<BranchModel> mergedBranches = new ArrayList<>();

                            progressCallback.updateProgress(0.1);
                            progressCallback.updateMessage("Перевірка гілок на змердженість...");

                            // First pass: check which branches are merged
                            for (BranchModel branch : allBranches) {
                                // Check if cancellation was requested
                                if (progressCallback.isCancelRequested()) {
                                    progressCallback.updateMessage("Операцію скасовано");
                                    return null;
                                }

                                // Update progress
                                double progress = 0.1 + (0.4 * processedBranches / totalBranches);
                                progressCallback.updateProgress(progress);
                                progressCallback.updateMessage("Перевірка гілки: " + branch.getName());

                                try {
                                    // Skip checking the main branch itself
                                    if (branch.getName().equals(finalMainBranch)) {
                                        processedBranches++;
                                        continue;
                                    }

                                    boolean isMerged = gitLabService.isCommitInMainBranch(currentProjectId, branch.getName(), finalMainBranch);
                                    if (isMerged) {
                                        // Check the date
                                        String lastCommitDateStr = branch.getLastCommit();
                                        if (lastCommitDateStr != null && !lastCommitDateStr.isEmpty()) {
                                            try {
                                                // The lastCommit is in ISO 8601 format, e.g. "2023-01-01T12:00:00Z"
                                                // We need to parse it to a LocalDate for comparison
                                                LocalDate lastCommitDate = LocalDate.parse(lastCommitDateStr.substring(0, 10));
                                                if (lastCommitDate.isBefore(finalCutoffDate) || lastCommitDate.isEqual(finalCutoffDate)) {
                                                    mergedBranches.add(branch);
                                                }
                                            } catch (Exception e) {
                                                logger.error("Error parsing last commit date: {}", lastCommitDateStr, e);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    logger.error("Error checking if branch is merged", e);
                                }

                                processedBranches++;
                            }

                            if (mergedBranches.isEmpty()) {
                                progressCallback.updateProgress(1.0);
                                progressCallback.updateMessage("Не знайдено змерджених гілок, які старіші за вказану дату");
                                return null;
                            }

                            // Show confirmation dialog
                            progressCallback.updateProgress(0.5);
                            progressCallback.updateMessage("Очікування підтвердження видалення...");

                            // We need to run this on the JavaFX thread and wait for the result
                            final List<BranchModel>[] confirmedBranchesArray = new List[1];
                            javafx.application.Platform.runLater(() -> {
                                confirmedBranchesArray[0] = DialogHelper.showDeleteConfirmationDialog(stage, mergedBranches);
                            });

                            // Wait for the dialog to be closed
                            while (confirmedBranchesArray[0] == null) {
                                // Check if cancellation was requested
                                if (progressCallback.isCancelRequested()) {
                                    progressCallback.updateMessage("Операцію скасовано");
                                    return null;
                                }

                                Thread.sleep(100);
                            }

                            List<BranchModel> confirmedBranches = confirmedBranchesArray[0];
                            if (confirmedBranches.isEmpty()) {
                                progressCallback.updateProgress(1.0);
                                progressCallback.updateMessage("Видалення скасовано");
                                return null;
                            }

                            // Second pass: delete confirmed branches
                            int totalConfirmed = confirmedBranches.size();
                            int deletedBranches = 0;

                            for (BranchModel branch : confirmedBranches) {
                                // Check if cancellation was requested
                                if (progressCallback.isCancelRequested()) {
                                    progressCallback.updateMessage("Операцію скасовано");
                                    break;
                                }

                                // Update progress
                                double progress = 0.6 + (0.4 * deletedBranches / totalConfirmed);
                                progressCallback.updateProgress(progress);
                                progressCallback.updateMessage("Видалення гілки: " + branch.getName());

                                gitLabService.deleteBranch(currentProjectId, branch.getName());
                                deletedBranches++;
                            }

                            progressCallback.updateProgress(1.0);
                            progressCallback.updateMessage("Видалення завершено");
                            return deletedBranches;
                        } catch (Exception e) {
                            logger.error("Error checking and deleting merged branches", e);
                            throw new RuntimeException(e);
                        }
                    },
                    deletedCount -> {
                        if (deletedCount != null && deletedCount > 0) {
                            // Refresh the branches list after deletion
                            refreshBranches();
                        }
                    }
            );
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
}
