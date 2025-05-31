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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DeleteConfirmationController {
    private static final Logger logger = LoggerFactory.getLogger(DeleteConfirmationController.class);

    @FXML
    private TableView<BranchModel> branchesTableView;

    @FXML
    private TableColumn<BranchModel, Boolean> selectedColumn;

    @FXML
    private TableColumn<BranchModel, String> nameColumn;

    @FXML
    private TableColumn<BranchModel, String> lastCommitColumn;

    @FXML
    private TableColumn<BranchModel, Boolean> mergeToDestColumn;

    @FXML
    private TableColumn<BranchModel, Boolean> defaultColumn;

    @FXML
    private TableColumn<BranchModel, Boolean> protectedColumn;

    @FXML
    private TableColumn<BranchModel, Boolean> mergedColumn;

    @FXML
    private TableColumn<BranchModel, Boolean> developersCanPushColumn;

    @FXML
    private TableColumn<BranchModel, Boolean> developersCanMergeColumn;

    @FXML
    private TableColumn<BranchModel, Boolean> canPushColumn;

    @FXML
    private Label branchCounterLabel;

    private Stage stage;
    private List<BranchModel> selectedBranches;

    public void initialize(List<BranchModel> branches, Stage stage) {
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

        // Setup boolean columns with icon display
        setupBooleanColumn(mergedColumn, "merged", "Змерджено");
        setupBooleanColumn(mergeToDestColumn, "mergedIntoTarget", "Змерджено в цільову");
        setupBooleanColumn(protectedColumn, "protected", "Захищена");
        setupBooleanColumn(developersCanPushColumn, "developersCanPush", "Розробник може пушити");
        setupBooleanColumn(developersCanMergeColumn, "developersCanMerge", "Розробник може мержити");
        setupBooleanColumn(canPushColumn, "canPush", "Можно пушити");
        setupBooleanColumn(defaultColumn, "default", "Гілка по замовченю");

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

        // Встановлення даних
        // Сортування гілок за назвою (не чутливо до регістру)
        branches.sort((b1, b2) -> String.CASE_INSENSITIVE_ORDER.compare(b1.getName(), b2.getName()));
        ObservableList<BranchModel> data = FXCollections.observableArrayList(branches);
        branchesTableView.setItems(data);

        // Add listeners to branch selection changes
        addBranchSelectionListeners(data);

        // Initialize branch counter
        updateBranchCounter();
    }

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

    @FXML
    private void deselectAll() {
        logger.debug("Deselecting all branches");
        branchesTableView.getItems().forEach(branch -> branch.setSelected(false));
        updateBranchCounter();
    }

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

    @FXML
    private void confirm() {
        logger.debug("Confirming deletion");
        selectedBranches = branchesTableView.getItems().stream()
                .filter(BranchModel::isSelected)
                .toList();
        stage.close();
    }

    @FXML
    private void cancel() {
        logger.debug("Cancelling deletion");
        selectedBranches = null;
        stage.close();
    }

    public List<BranchModel> getSelectedBranches() {
        return selectedBranches;
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
     * Sets up a boolean column to display icons (★ for true, space for false) with tooltips
     * 
     * @param column The TableColumn to set up
     * @param propertyName The name of the property in the BranchModel
     * @param trueTooltip The tooltip text for true values
     */
    private void setupBooleanColumn(TableColumn<BranchModel, Boolean> column, String propertyName, 
                                   String trueTooltip) {
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
