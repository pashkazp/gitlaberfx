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
import com.depavlo.gitlaberfx.util.I18nUtil;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Controller for the filter panel component.
 * This panel allows users to filter branches by name using regular expressions.
 */
public class FilterPanelController {
    private static final Logger logger = LoggerFactory.getLogger(FilterPanelController.class);

    @FXML
    private TextField filterTextField;

    @FXML
    private Button includeButton;

    @FXML
    private Button excludeButton;

    private ObservableList<BranchModel> targetBranches;

    /**
     * Initializes the controller.
     * Sets up tooltips for the buttons.
     */
    @FXML
    public void initialize() {
        // Set tooltips
        includeButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.filter.include")));
        excludeButton.setTooltip(new Tooltip(I18nUtil.getMessage("tooltip.filter.exclude")));
    }

    /**
     * Sets the target list of branches to filter.
     *
     * @param branches The list of branches to filter
     */
    public void setTargetList(ObservableList<BranchModel> branches) {
        this.targetBranches = branches;
        // Disable the panel if no branches are available
        boolean hasTargetBranches = branches != null && !branches.isEmpty();
        filterTextField.setDisable(!hasTargetBranches);
        includeButton.setDisable(!hasTargetBranches);
        excludeButton.setDisable(!hasTargetBranches);
    }

    /**
     * Selects all branches that match the filter pattern.
     */
    @FXML
    public void includeMatching() {
        applyFilter(true);
    }

    /**
     * Deselects all branches that match the filter pattern.
     */
    @FXML
    public void excludeMatching() {
        applyFilter(false);
    }

    /**
     * Applies the filter to the target branches.
     *
     * @param include Whether to include (select) or exclude (deselect) matching branches
     */
    private void applyFilter(boolean include) {
        if (targetBranches == null || targetBranches.isEmpty()) {
            return;
        }

        String filterText = filterTextField.getText().trim();
        if (filterText.isEmpty()) {
            return;
        }

        try {
            Pattern pattern = Pattern.compile(filterText);
            
            for (BranchModel branch : targetBranches) {
                if (pattern.matcher(branch.getName()).matches()) {
                    branch.setSelected(include);
                }
            }
        } catch (PatternSyntaxException e) {
            logger.error("Invalid regex pattern: {}", filterText, e);
            // Show error dialog
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(I18nUtil.getMessage("error.filter.regex.title"));
            alert.setHeaderText(null);
            alert.setContentText(I18nUtil.getMessage("error.filter.regex.message"));
            alert.showAndWait();
        }
    }
}