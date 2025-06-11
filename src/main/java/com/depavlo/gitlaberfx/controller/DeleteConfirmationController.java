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

import com.depavlo.gitlaberfx.model.BranchModel;
import com.depavlo.gitlaberfx.model.OperationConfirmationResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.depavlo.gitlaberfx.util.I18nUtil;

import java.util.List;

/**
 * Controller for the delete confirmation dialog.
 * This class handles the functionality of the dialog that allows users to select
 * branches for deletion. It displays a table of branches with various properties
 * and provides options to select, deselect, or invert the selection of branches.
 * Users can confirm or cancel the deletion operation.
 */
public class DeleteConfirmationController {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(DeleteConfirmationController.class);

    /** Table view that displays the branches. */
    @FXML
    private TableView<BranchModel> branchesTableView;

    /** Column for the selection checkboxes. */
    @FXML
    private TableColumn<BranchModel, Boolean> selectedColumn;

    /** Column for the branch names. */
    @FXML
    private TableColumn<BranchModel, String> nameColumn;

    /** Column for the last commit information. */
    @FXML
    private TableColumn<BranchModel, String> lastCommitColumn;

    /** Column indicating whether the branch is merged into the target branch. */
    @FXML
    private TableColumn<BranchModel, Boolean> mergeToDestColumn;

    /** Column indicating whether the branch is the default branch. */
    @FXML
    private TableColumn<BranchModel, Boolean> defaultColumn;

    /** Column indicating whether the branch is protected. */
    @FXML
    private TableColumn<BranchModel, Boolean> protectedColumn;

    /** Column indicating whether the branch is merged. */
    @FXML
    private TableColumn<BranchModel, Boolean> mergedColumn;

    /** Column indicating whether developers can push to the branch. */
    @FXML
    private TableColumn<BranchModel, Boolean> developersCanPushColumn;

    /** Column indicating whether developers can merge to the branch. */
    @FXML
    private TableColumn<BranchModel, Boolean> developersCanMergeColumn;

    /** Column indicating whether the current user can push to the branch. */
    @FXML
    private TableColumn<BranchModel, Boolean> canPushColumn;

    /** Label that displays the count of selected branches. */
    @FXML
    private Label branchCounterLabel;

    /** The stage that contains the delete confirmation dialog. */
    private Stage stage;

    /** The list of branches that are selected for operation (deletion or archiving). */
    private List<BranchModel> selectedBranches;

    /** The type of deletion operation. */
    private String deletionType;

    /** The name of the project from which branches will be deleted. */
    private String projectName;

    /** Label that displays the deletion type and project name. */
    @FXML
    private Label deletionInfoLabel;

    /** Checkbox for selecting archive option instead of delete. */
    @FXML
    private CheckBox archiveCheckBox;

    /** Button for confirming the operation (delete or archive). */
    @FXML
    private Button confirmButton;

    /** Flag indicating whether to archive branches instead of deleting them. */
    private boolean archive = true;

    /**
     * Initializes the controller with the list of branches and the stage (backward compatibility method).
     * This method is provided for backward compatibility with code that doesn't provide
     * deletion type and project name.
     *
     * @param branches The list of branches to display in the table
     * @param stage The stage that contains the delete confirmation dialog
     */
    public void initialize(List<BranchModel> branches, Stage stage) {
        initialize(branches, stage, I18nUtil.getMessage("main.delete.selected"), "");
    }

    /**
     * Initializes the controller with the list of branches, the stage, deletion type, and project name.
     * This method is called after the FXML has been loaded.
     * It sets up the table columns, populates the table with branches,
     * adds listeners for branch selection changes, and displays the deletion type and project name.
     *
     * @param branches The list of branches to display in the table
     * @param stage The stage that contains the delete confirmation dialog
     * @param deletionType The type of deletion operation (e.g., "Deleting selected branches")
     * @param projectName The name of the project from which branches will be deleted
     */
    public void initialize(List<BranchModel> branches, Stage stage, String deletionType, String projectName) {
        logger.debug("initialize: branches.size={}, stage={}, deletionType={}, projectName={}", 
                branches != null ? branches.size() : "null", 
                stage != null ? "not null" : "null",
                deletionType,
                projectName);
        this.stage = stage;
        this.deletionType = deletionType;
        this.projectName = projectName;

        // Set the deletion info label text
        if (deletionInfoLabel != null) {
            deletionInfoLabel.setText(I18nUtil.getMessage("delete.confirmation.info", deletionType, projectName));
        }

        // Налаштування колонок таблиці
        selectedColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectedColumn.setCellFactory(column -> {
            CheckBoxTableCell<BranchModel, Boolean> cell = new CheckBoxTableCell<BranchModel, Boolean>() {
                @Override
                public void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        TableRow<?> row = getTableRow();
                        if (row != null && row.getItem() != null) {
                            BranchModel branch = (BranchModel) row.getItem();
                            // Disable checkbox for protected branches
                            setDisable(branch.isProtected());
                        }
                    }
                }
            };
            cell.setEditable(true);
            return cell;
        });

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        lastCommitColumn.setCellValueFactory(new PropertyValueFactory<>("lastCommit"));

        // Setup boolean columns with icon display
        setupBooleanColumn(mergedColumn, "merged", I18nUtil.getMessage("column.tooltip.merged"));
        setupBooleanColumn(mergeToDestColumn, "mergedIntoTarget", I18nUtil.getMessage("column.tooltip.merged.into.target"));
        setupBooleanColumn(protectedColumn, "protected", I18nUtil.getMessage("column.tooltip.protected"));
        setupBooleanColumn(developersCanPushColumn, "developersCanPush", I18nUtil.getMessage("column.tooltip.developers.can.push"));
        setupBooleanColumn(developersCanMergeColumn, "developersCanMerge", I18nUtil.getMessage("column.tooltip.developers.can.merge"));
        setupBooleanColumn(canPushColumn, "canPush", I18nUtil.getMessage("column.tooltip.can.push"));
        setupBooleanColumn(defaultColumn, "default", I18nUtil.getMessage("column.tooltip.default"));

        // Налаштування TableView для редагування
        branchesTableView.setEditable(true);

        // Додавання обробника клавіш для перемикання чекбоксів пробілом
        branchesTableView.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.SPACE) {
                BranchModel selectedBranch = branchesTableView.getSelectionModel().getSelectedItem();
                if (selectedBranch != null && !selectedBranch.isProtected()) {
                    selectedBranch.setSelected(!selectedBranch.isSelected());
                    updateBranchCounter();
                    event.consume();
                }
            }
        });

        // Встановлення даних
        // Сортування гілок за назвою (не чутливо до регістру)
        branches.sort((b1, b2) -> String.CASE_INSENSITIVE_ORDER.compare(b1.getName(), b2.getName()));
        ObservableList<BranchModel> data = FXCollections.observableArrayList(branches);
        branchesTableView.setItems(data);

        // Add listeners to branch selection changes
        addBranchSelectionListeners(data);

        // Initialize branch counter
        updateBranchCounter();

        // Set up archive checkbox listener
        if (archiveCheckBox != null) {
            archiveCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                archive = newValue;
                updateConfirmButtonText();
            });
        }
    }

    /**
     * Updates the text on the confirm button based on the archive checkbox state.
     * If the archive checkbox is selected, the button text changes to "Archive selected".
     * Otherwise, it shows "Delete selected".
     */
    private void updateConfirmButtonText() {
        if (confirmButton != null) {
            if (archive) {
                confirmButton.setText(I18nUtil.getMessage("delete.confirmation.archive.selected"));
                confirmButton.setStyle("-fx-base: #0066cc;"); // Blue for archive
            } else {
                confirmButton.setText(I18nUtil.getMessage("delete.confirmation.delete.selected"));
                confirmButton.setStyle("-fx-base: #ff0000;"); // Red for delete
            }
        }
    }

    /**
     * Selects all non-protected branches in the table.
     * This method is called when the user clicks the "Select All" button.
     * Protected branches cannot be selected for deletion.
     */
    @FXML
    private void selectAll() {
        logger.debug("Selecting all branches");
        branchesTableView.getItems().forEach(branch -> {
            if (!branch.isProtected()) {
                branch.setSelected(true);
            }
        });
        updateBranchCounter();
    }

    /**
     * Deselects all branches in the table.
     * This method is called when the user clicks the "Deselect All" button.
     */
    @FXML
    private void deselectAll() {
        logger.debug("Deselecting all branches");
        branchesTableView.getItems().forEach(branch -> branch.setSelected(false));
        updateBranchCounter();
    }

    /**
     * Inverts the selection of all non-protected branches in the table.
     * This method is called when the user clicks the "Invert Selection" button.
     * Selected branches become deselected, and deselected branches become selected.
     * Protected branches remain unselected.
     */
    @FXML
    private void invertSelection() {
        logger.debug("Inverting selection");
        branchesTableView.getItems().forEach(branch -> {
            if (!branch.isProtected()) {
                branch.setSelected(!branch.isSelected());
            }
        });
        updateBranchCounter();
    }

    /**
     * Confirms the branch operation selection and closes the dialog.
     * This method is called when the user clicks the confirm button.
     * It saves the list of selected branches and the archive flag, then closes the dialog.
     */
    @FXML
    private void confirm() {
        logger.debug("Confirming operation, archive={}", archive);
        selectedBranches = branchesTableView.getItems().stream()
                .filter(BranchModel::isSelected)
                .toList();
        stage.close();
    }

    /**
     * Cancels the branch deletion operation and closes the dialog.
     * This method is called when the user clicks the cancel button.
     * It sets the selected branches to null and closes the dialog.
     */
    @FXML
    private void cancel() {
        logger.debug("Cancelling deletion");
        selectedBranches = null;
        stage.close();
    }

    /**
     * Gets the operation confirmation result containing the selected branches and archive flag.
     * This method is called after the dialog is closed to retrieve the operation details.
     *
     * @return The OperationConfirmationResult containing the selected branches and archive flag,
     *         or null if the operation was cancelled
     */
    public OperationConfirmationResult getSelectedBranches() {
        logger.debug("getSelectedBranches: selectedBranches.size={}, archive={}", 
                selectedBranches != null ? selectedBranches.size() : "null", archive);

        if (selectedBranches == null) {
            return null;
        }

        return new OperationConfirmationResult(selectedBranches, archive);
    }

    /**
     * Updates the branch counter label with the current count of selected branches and total branches.
     * This method is safe to call from any thread.
     */
    private void updateBranchCounter() {
        logger.debug("updateBranchCounter");
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
        logger.debug("addBranchSelectionListeners: branches.size={}", branches != null ? branches.size() : "null");
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
     * Sets up a boolean column to display icons (★ for true, space for false) with tooltips
     * 
     * @param column The TableColumn to set up
     * @param propertyName The name of the property in the BranchModel
     * @param trueTooltip The tooltip text for true values
     */
    private void setupBooleanColumn(TableColumn<BranchModel, Boolean> column, String propertyName, 
                                   String trueTooltip) {
        logger.debug("setupBooleanColumn: propertyName={}, trueTooltip={}", propertyName, trueTooltip);
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setCellFactory(col -> new TableCell<BranchModel, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setTooltip(new Tooltip(trueTooltip));
                setAlignment(javafx.geometry.Pos.CENTER);

                if (empty || item == null) {
                    setText(null);
                } else {
                    // Use star symbol for true, space for false
                    setText(item ? "🗸" : " ");
                }
            }
        });
    }
}
