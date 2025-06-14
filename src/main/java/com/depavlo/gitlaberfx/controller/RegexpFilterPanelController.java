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
 */package com.depavlo.gitlaberfx.controller;

import com.depavlo.gitlaberfx.model.BranchModel;
import com.depavlo.gitlaberfx.model.UIStateModel;
import com.depavlo.gitlaberfx.util.I18nUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * Controller for the filter panel component.
 * This class is responsible for managing the filter panel UI,
 * which allows users to filter branches in the main table view.
 */
public class RegexpFilterPanelController {

    @FXML
    private TextField filterTextField;

    @FXML
    private Button includeButton;

    @FXML
    private Button excludeButton;

    private UIStateModel uiStateModel;

    private javafx.collections.ObservableList<BranchModel> targetList;

    /**
     * Sets the target list of branches for filtering operations.
     * This method provides null safety by using an empty list when null is provided.
     *
     * @param branches The list of branches to set as the target list
     */
    public void setTargetList(javafx.collections.ObservableList<BranchModel> branches) {
        this.targetList = (branches != null) ? branches : javafx.collections.FXCollections.emptyObservableList();
    }

    /**
     * Initializes the controller with the UI state model.
     * This method sets up bindings to enable/disable the filter components
     * based on whether there are branches in the table view.
     *
     * @param uiStateModel The UI state model containing branch data
     */
    public void initialize(UIStateModel uiStateModel) {
        this.uiStateModel = uiStateModel;

        // Create a binding that is true when there are no branches
        BooleanBinding noBranches = Bindings.isEmpty(uiStateModel.getCurrentProjectBranches());

        // Bind the disable property of the components to the noBranches binding
        filterTextField.editableProperty().bind(noBranches.not());
        includeButton.disableProperty().bind(noBranches);
        excludeButton.disableProperty().bind(noBranches);
    }

    /**
     * Sets the disabled state of all filter panel components.
     * This method is used to disable the filter panel when the application is busy.
     *
     * @param disabled true to disable the components, false to enable them
     */
    public void setDisabled(boolean disabled) {
        // Unbind the disable property to allow manual setting
        filterTextField.editableProperty().unbind();
        includeButton.disableProperty().unbind();
        excludeButton.disableProperty().unbind();

        // Set the disabled state
        filterTextField.setEditable(!disabled);
        includeButton.setDisable(disabled);
        excludeButton.setDisable(disabled);

        // If not disabled, rebind to the noBranches binding
        if (!disabled) {
            BooleanBinding noBranches = Bindings.isEmpty(this.targetList);
            filterTextField.editableProperty().bind(noBranches.not());
            includeButton.disableProperty().bind(noBranches);
            excludeButton.disableProperty().bind(noBranches);
        }
    }

    /**
     * Handles the action when the include button is clicked.
     * This method gets the filter text, creates a regex pattern from it,
     * and sets the selected property to true for all branches whose names match the pattern.
     * It works with the dataset of the current window (main window or delete confirmation window).
     */
    @FXML
    public void includeMatching() {
        String filterText = filterTextField.getText();
        if (filterText == null || filterText.isEmpty()) {
            return;
        }

        try {
            // Create a regex pattern from the filter text
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(filterText);

            // Use the target list directly
            if (targetList != null && !targetList.isEmpty()) {
                for (BranchModel branch : targetList) {
                    if (pattern.matcher(branch.getName()).matches() && !branch.isProtected()) {
                        branch.setSelected(true);
                    }
                }
            } else {
                // Fallback to UIStateModel's dataset if targetList is not set
                for (BranchModel branch : uiStateModel.getCurrentProjectBranches()) {
                    if (pattern.matcher(branch.getName()).matches() && !branch.isProtected()) {
                        branch.setSelected(true);
                    }
                }
            }
        } catch (java.util.regex.PatternSyntaxException e) {
            handlePatternSyntaxException(e);
        }
    }

    /**
     * Handles the action when the exclude button is clicked.
     * This method gets the filter text, creates a regex pattern from it,
     * and sets the selected property to false for all branches whose names match the pattern.
     * It works with the dataset of the current window (main window or delete confirmation window).
     */
    @FXML
    public void excludeMatching() {
        String filterText = filterTextField.getText();
        if (filterText == null || filterText.isEmpty()) {
            return;
        }

        try {
            // Create a regex pattern from the filter text
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(filterText);

            // Use the target list directly
            if (targetList != null && !targetList.isEmpty()) {
                for (BranchModel branch : targetList) {
                    if (pattern.matcher(branch.getName()).matches() && !branch.isProtected()) {
                        branch.setSelected(false);
                    }
                }
            } else {
                // Fallback to UIStateModel's dataset if targetList is not set
                for (BranchModel branch : uiStateModel.getCurrentProjectBranches()) {
                    if (pattern.matcher(branch.getName()).matches() && !branch.isProtected()) {
                        branch.setSelected(false);
                    }
                }
            }
        } catch (java.util.regex.PatternSyntaxException e) {
            handlePatternSyntaxException(e);
        }
    }

    /**
     * Handles PatternSyntaxException in a user-friendly way.
     * This method displays an error alert with a localized message explaining the issue
     * with the regular expression pattern.
     *
     * @param e The PatternSyntaxException that was thrown
     */
    private void handlePatternSyntaxException(java.util.regex.PatternSyntaxException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nUtil.getMessage("error.filter.regex.title"));
        alert.setHeaderText(I18nUtil.getMessage("error.filter.regex.message"));

        // Create a more user-friendly explanation of the error
        String errorMessage = e.getMessage();
        String errorDetails;

        // Provide user-friendly localized messages for common regex errors
        if (errorMessage.contains("Unclosed group near index")) {
            errorDetails = I18nUtil.getMessage("error.filter.regex.unclosed.group");
        } else if (errorMessage.contains("Dangling meta character")) {
            errorDetails = I18nUtil.getMessage("error.filter.regex.dangling.meta");
        } else if (errorMessage.contains("Illegal repetition")) {
            errorDetails = I18nUtil.getMessage("error.filter.regex.illegal.repetition");
        } else {
            // For other errors, provide a generic message with the error details
            errorDetails = I18nUtil.getMessage("error.filter.regex.default", errorMessage);
        }

        alert.setContentText(errorDetails);
        alert.showAndWait();
    }
}
