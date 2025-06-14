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

import com.depavlo.gitlaberfx.controller.DateSelectorController.DateSelectorResult;
import com.depavlo.gitlaberfx.model.BranchModel;
import com.depavlo.gitlaberfx.model.UIStateModel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the date filter panel.
 * This panel allows filtering branches by their last commit date.
 */
public class DateFilterPanelController {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(DateFilterPanelController.class);

    /** Text field displaying the selected date range. */
    @FXML
    private TextField dateRangeTextField;

    /** Button to open the date selector. */
    @FXML
    private Button datePickerButton;

    /** Button to include branches matching the date criteria. */
    @FXML
    private Button includeDateFilerButton;

    /** Button to exclude branches matching the date criteria. */
    @FXML
    private Button excludeDateFilerButton;

    /** The UI state model containing branch data. */
    private UIStateModel uiStateModel;

    /** The list of branches to filter. */
    private ObservableList<BranchModel> branches;

    /** The date after which branches should be included. */
    private LocalDate dateAfter;

    /** The date before which branches should be included. */
    private LocalDate dateBefore;

    /** The formatter for displaying dates in the system format. */
    private DateTimeFormatter dateFormatter;

    /** Binding that is true when there are no branches. */
    private BooleanBinding noBranches;

    /**
     * Initializes the controller.
     */
    @FXML
    private void initialize() {
        logger.debug("Initializing DateFilterPanelController");
        dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
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
        noBranches = Bindings.isEmpty(uiStateModel.getCurrentProjectBranches());

        // Bind the disable property of the components to the noBranches binding
        includeDateFilerButton.disableProperty().bind(noBranches);
        excludeDateFilerButton.disableProperty().bind(noBranches);

        // The include and exclude buttons should be disabled if there are no branches
        // or if there are no date criteria
        updateButtonState();
    }

    /**
     * Sets the list of branches to filter.
     *
     * @param branches the list of branches
     */
    public void setBranches(javafx.collections.ObservableList<BranchModel> branches) {
        this.branches = (branches != null) ? branches : javafx.collections.FXCollections.emptyObservableList();
    }

    /**
     * Sets the disabled state of all date filter panel components.
     * This method is used to disable the filter panel when the application is busy.
     *
     * @param disabled true to disable the components, false to enable them
     */
    public void setDisabled(boolean disabled) {
        // Unbind the disable property to allow manual setting
        includeDateFilerButton.disableProperty().unbind();
        excludeDateFilerButton.disableProperty().unbind();

        // Set the disabled state
        includeDateFilerButton.setDisable(disabled);
        excludeDateFilerButton.setDisable(disabled);

        if (!disabled) {
            BooleanBinding noBranches = Bindings.isEmpty(this.branches);
            includeDateFilerButton.disableProperty().bind(noBranches);
            excludeDateFilerButton.disableProperty().bind(noBranches);
        }
    }

    /**
     * Opens the date selector dialog.
     */
    @FXML
    private void openDatePicker() {
        logger.debug("Opening date picker");
        Window owner = dateRangeTextField.getScene().getWindow();

        DateSelectorResult result = DateSelectorController.Builder.create()
                .title("Date Selector")
                .owner(owner)
                .dateAfter(dateAfter)
                .dateBefore(dateBefore)
                .build();

        if (result != null && result.isOk() && result.isValidated()) {
            dateAfter = result.getDateAfter();
            dateBefore = result.getDateBefore();
            updateDateRangeTextField();
            updateButtonState();
        }
    }

    /**
     * Updates the text field displaying the selected date range.
     */
    private void updateDateRangeTextField() {
        if (dateAfter == null && dateBefore == null) {
            dateRangeTextField.setText("");
        } else if (dateAfter != null && dateBefore == null) {
            dateRangeTextField.setText("→ " + dateAfter.format(dateFormatter)  );
        } else if (dateAfter == null && dateBefore != null) {
            dateRangeTextField.setText(dateBefore.format(dateFormatter) + " ←");
        } else {
            dateRangeTextField.setText("→ " + dateAfter.format(dateFormatter) + " - " + dateBefore.format(dateFormatter) + " ←");
        }
    }

    /**
     * Updates the state of the include and exclude buttons.
     * The buttons are disabled if there are no branches or if there are no date criteria.
     */
    private void updateButtonState() {
        boolean hasDateCriteria = dateAfter != null || dateBefore != null;

        // Unbind the disable property to allow manual setting
        includeDateFilerButton.disableProperty().unbind();
        excludeDateFilerButton.disableProperty().unbind();

        if (noBranches != null) {
            // If noBranches is available, bind the disable property to a combination of noBranches and !hasDateCriteria
            includeDateFilerButton.disableProperty().bind(noBranches.or(Bindings.createBooleanBinding(() -> !hasDateCriteria)));
            excludeDateFilerButton.disableProperty().bind(noBranches.or(Bindings.createBooleanBinding(() -> !hasDateCriteria)));
        } else {
            // If noBranches is not available, just use hasDateCriteria
            includeDateFilerButton.setDisable(!hasDateCriteria);
            excludeDateFilerButton.setDisable(!hasDateCriteria);
        }
    }

    /**
     * Includes branches matching the date criteria.
     */
    @FXML
    private void includeDateFilerMatching() {
        logger.debug("Including branches matching date criteria");
        if (branches == null) {
            logger.warn("Branch list is null");
            return;
        }

        for (BranchModel branch : branches) {
            if (matchesDateCriteria(branch)) {
                branch.setSelected(true);
            }
        }
    }

    /**
     * Excludes branches matching the date criteria.
     */
    @FXML
    private void excludeDateFilerMatching() {
        logger.debug("Excluding branches matching date criteria");
        if (branches == null) {
            logger.warn("Branch list is null");
            return;
        }

        for (BranchModel branch : branches) {
            if (matchesDateCriteria(branch)) {
                branch.setSelected(false);
            }
        }
    }

    /**
     * Checks if a branch matches the date criteria.
     *
     * @param branch the branch to check
     * @return true if the branch matches the criteria, false otherwise
     */
    private boolean matchesDateCriteria(BranchModel branch) {
        LocalDate commitDate = parseCommitDate(branch.getLastCommit());
        if (commitDate == null) {
            return false;
        }

        if (dateAfter != null && dateBefore != null) {
            // Both dates are set, check if commit date is in range
            return !commitDate.isBefore(dateAfter) && commitDate.isBefore(dateBefore.plusDays(1));
        } else if (dateAfter != null) {
            // Only dateAfter is set
            return !commitDate.isBefore(dateAfter);
        } else if (dateBefore != null) {
            // Only dateBefore is set
            return commitDate.isBefore(dateBefore.plusDays(1));
        }

        // No date criteria
        return false;
    }

    /**
     * Parses the commit date from the lastCommit field of a branch.
     *
     * @param lastCommit the lastCommit field value
     * @return the parsed date, or null if parsing failed
     */
    private LocalDate parseCommitDate(String lastCommit) {
        if (lastCommit == null || lastCommit.isEmpty()) {
            return null;
        }

        try {
            // Parse ISO 8601 date with time zone
            OffsetDateTime dateTime = OffsetDateTime.parse(lastCommit);
            return dateTime.toLocalDate();
        } catch (Exception e) {
            logger.warn("Failed to parse commit date: {}", lastCommit, e);
            return null;
        }
    }
}
