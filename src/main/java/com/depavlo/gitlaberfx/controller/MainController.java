package com.depavlo.gitlaberfx.controller;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.model.BranchModel;
import com.depavlo.gitlaberfx.service.GitLabService;
import com.depavlo.gitlaberfx.util.DialogHelper;
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

        try {
            gitLabService.connect();

            List<GitLabService.Project> projects = gitLabService.getProjects();
            projectComboBox.setItems(FXCollections.observableArrayList(
                    projects.stream()
                            .map(GitLabService.Project::getName)
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.toList())
            ));
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            showError("Помилка завантаження", "Не вдалося завантажити налаштування: " + e.getMessage());
        }
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
            try {
                List<GitLabService.Project> projects = null;
                try {
                    projects = gitLabService.getProjects();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                GitLabService.Project selectedProject = projects.stream()
                        .filter(p -> p.getName().equals(projectName))
                        .findFirst()
                        .orElse(null);

                if (selectedProject != null) {
                    currentProjectId = String.valueOf(selectedProject.getId());
                    config.save();

                    List<BranchModel> branches = gitLabService.getBranches(currentProjectId);
                    // Сортування гілок за назвою (не чутливо до регістру)
                    branches.sort((b1, b2) -> String.CASE_INSENSITIVE_ORDER.compare(b1.getName(), b2.getName()));
                    branchesTableView.setItems(FXCollections.observableArrayList(branches));

                    // Add branch names to the list that already contains "not selected" item
                    branchNames.addAll(
                            branches.stream()
                                    .map(BranchModel::getName)
                                    .sorted(String.CASE_INSENSITIVE_ORDER)
                                    .collect(Collectors.toList())
                    );
                    mainBranchComboBox.setItems(FXCollections.observableArrayList(branchNames));
                    mainBranchComboBox.setValue(NOT_SELECTED_ITEM);
                }
            } catch (IOException e) {
                logger.error("Error loading project branches", e);
                showError("Помилка завантаження", "Не вдалося завантажити гілки: " + e.getMessage());
            }
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
                    // Check if branches have been merged into the selected main branch
                    for (BranchModel branch : branches) {
                        try {
                            // Skip checking the main branch itself
                            if (branch.getName().equals(mainBranch)) {
                                branch.setMerged(false);
                                continue;
                            }
                            boolean isMerged = gitLabService.isCommitInMainBranch(currentProjectId, branch.getName(), mainBranch);
                            branch.setMerged(isMerged);
                        } catch (IOException e) {
                            logger.error("Error checking if branch {} is merged into {}", branch.getName(), mainBranch, e);
                            branch.setMerged(false);
                        }
                    }
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
        System.exit(0);
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
                try {
                    for (BranchModel branch : confirmedBranches) {
                        gitLabService.deleteBranch(currentProjectId, branch.getName());
                    }
                    refreshBranches();
                } catch (IOException e) {
                    logger.error("Error deleting branches", e);
                    showError("Помилка видалення", "Не вдалося видалити гілки: " + e.getMessage());
                }
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

        LocalDate date = DialogHelper.showDatePickerDialog(stage);
        if (date != null) {
            try {
                List<BranchModel> mergedBranches = branchesTableView.getItems().stream()
                        .filter(branch -> {
                            try {
                                return gitLabService.isCommitInMainBranch(currentProjectId, branch.getName(), mainBranch);
                            } catch (IOException e) {
                                logger.error("Error checking if branch is merged", e);
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

                if (!mergedBranches.isEmpty()) {
                    List<BranchModel> confirmedBranches = DialogHelper.showDeleteConfirmationDialog(stage, mergedBranches);
                    if (confirmedBranches != null && !confirmedBranches.isEmpty()) {
                        for (BranchModel branch : confirmedBranches) {
                            gitLabService.deleteBranch(currentProjectId, branch.getName());
                        }
                        refreshBranches();
                    }
                } else {
                    showInfo("Інформація", "Не знайдено змерджених гілок");
                }
            } catch (IOException e) {
                logger.error("Error checking merged branches", e);
                showError("Помилка", "Не вдалося перевірити гілки: " + e.getMessage());
            }
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
