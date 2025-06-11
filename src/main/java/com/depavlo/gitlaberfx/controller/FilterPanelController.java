package com.depavlo.gitlaberfx.controller;

import com.depavlo.gitlaberfx.model.BranchModel;
import com.depavlo.gitlaberfx.model.UIStateModel;
import com.depavlo.gitlaberfx.util.I18nUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Controller for the filter panel component.
 * This class is responsible for managing the filter panel UI,
 * which allows users to filter branches in the main table view.
 */
public class FilterPanelController {

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
        filterTextField.disableProperty().bind(noBranches);
        includeButton.disableProperty().bind(noBranches);
        excludeButton.disableProperty().bind(noBranches);
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

            // Find the parent TableView in the scene graph
            javafx.scene.Node node = filterTextField.getScene().lookup("#branchesTableView");
            if (node instanceof TableView) {
                // If we found a TableView, use its items as the dataset
                @SuppressWarnings("unchecked")
                TableView<BranchModel> tableView = (TableView<BranchModel>) node;
                for (BranchModel branch : tableView.getItems()) {
                    if (pattern.matcher(branch.getName()).matches() && !branch.isProtected()) {
                        branch.setSelected(true);
                    }
                }
            } else {
                // If we couldn't find a TableView, use the UIStateModel's dataset
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

            // Find the parent TableView in the scene graph
            javafx.scene.Node node = filterTextField.getScene().lookup("#branchesTableView");
            if (node instanceof TableView) {
                // If we found a TableView, use its items as the dataset
                @SuppressWarnings("unchecked")
                TableView<BranchModel> tableView = (TableView<BranchModel>) node;
                for (BranchModel branch : tableView.getItems()) {
                    if (pattern.matcher(branch.getName()).matches() && !branch.isProtected()) {
                        branch.setSelected(false);
                    }
                }
            } else {
                // If we couldn't find a TableView, use the UIStateModel's dataset
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
