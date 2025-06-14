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

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for the universal date selector dialog.
 * This class handles the functionality of the date selector dialog, which allows
 * the user to select a date range or a single date.
 */
public class DateSelectorController {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(DateSelectorController.class);

    /** The date picker for the "date after" value. */
    @FXML
    private DatePicker dateAfterPicker;

    /** The date picker for the "date before" value. */
    @FXML
    private DatePicker dateBeforePicker;

    /** The stage that contains the date selector dialog. */
    private Stage stage;

    /** The minimum allowed date. */
    private LocalDate minDate;

    /** The maximum allowed date. */
    private LocalDate maxDate;

    /** The result of the date selection. */
    private DateSelectorResult result;

    /** The date formatter for parsing and formatting dates. */
    private DateTimeFormatter dateFormatter;

    /**
     * Initializes the controller with the stage and initial values.
     * This method is called after the FXML has been loaded.
     *
     * @param stage The stage that contains the date selector dialog
     * @param initialDateAfter The initial "date after" value, may be null
     * @param initialDateBefore The initial "date before" value, may be null
     * @param minDate The minimum allowed date, may be null
     * @param maxDate The maximum allowed date, may be null
     */
    public void initialize(Stage stage, LocalDate initialDateAfter, LocalDate initialDateBefore, 
                          LocalDate minDate, LocalDate maxDate) {
        logger.debug("initialize: stage={}, initialDateAfter={}, initialDateBefore={}, minDate={}, maxDate={}",
                stage != null ? "not null" : "null",
                initialDateAfter, initialDateBefore, minDate, maxDate);

        this.stage = stage;
        this.minDate = minDate;
        this.maxDate = maxDate;

        // Set up the date formatter
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Set up date constraints if provided
        if (minDate != null || maxDate != null) {
            setupDateCellFactory(dateAfterPicker);
            setupDateCellFactory(dateBeforePicker);
        }

        // Set initial values if provided
        dateAfterPicker.setValue(initialDateAfter);
        dateBeforePicker.setValue(initialDateBefore);

        // Set up listeners
        setupListeners();

        // Set initial result
        result = new DateSelectorResult(null, null, true, CompletionType.CANCEL);
    }

    /**
     * Sets up a date cell factory for a DatePicker to enforce date constraints.
     */
    private void setupDateCellFactory(DatePicker picker) {
        picker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || 
                          (minDate != null && date.isBefore(minDate)) || 
                          (maxDate != null && date.isAfter(maxDate)));
            }
        });
    }

    /**
     * Sets up listeners for the UI components.
     */
    private void setupListeners() {
        // Add listeners to validate dates when they change
        dateAfterPicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
        dateBeforePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
    }

    /**
     * Validates the dates selected in the DatePickers.
     * This method checks if the dates satisfy the constraints.
     * It also updates the visual feedback for the user.
     */
    private void validateDates() {
        LocalDate dateAfter = dateAfterPicker.getValue();
        LocalDate dateBefore = dateBeforePicker.getValue();

        boolean valid = true;

        // Check if "date after" is before "date before"
        if (dateAfter != null && dateBefore != null && dateAfter.isAfter(dateBefore)) {
            valid = false;
            // Provide visual feedback
            dateAfterPicker.setStyle("-fx-background-color: #FF5555;");
            dateBeforePicker.setStyle("-fx-background-color: #FF5555;");
        } else {
            dateAfterPicker.setStyle("");
            dateBeforePicker.setStyle("");
        }
    }

    /**
     * Confirms the date selection and closes the dialog.
     * This method is called when the user clicks the confirm button.
     * It validates the dates, saves the result, and closes the dialog.
     */
    @FXML
    private void confirm() {
        logger.debug("Confirming date selection");

        // Get the dates directly from the DatePickers
        LocalDate dateAfter = dateAfterPicker.getValue();
        LocalDate dateBefore = dateBeforePicker.getValue();

        // Validate the dates
        boolean valid = validateDateRange(dateAfter, dateBefore);

        // Create the result
        result = new DateSelectorResult(dateAfter, dateBefore, valid, CompletionType.OK);

        // Close the dialog
        stage.close();
    }

    /**
     * Cancels the date selection and closes the dialog.
     * This method is called when the user clicks the cancel button.
     */
    @FXML
    private void cancel() {
        logger.debug("Cancelling date selection");
        result = new DateSelectorResult(null, null, true, CompletionType.CANCEL);
        stage.close();
    }

    /**
     * Parses a date string into a LocalDate.
     *
     * @param dateStr The date string to parse
     * @return The parsed date, or null if the string is empty or invalid
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, dateFormatter);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse date: {}", dateStr, e);
            return null;
        }
    }

    /**
     * Validates a date range.
     *
     * @param dateAfter The "date after" value
     * @param dateBefore The "date before" value
     * @return true if the date range is valid, false otherwise
     */
    private boolean validateDateRange(LocalDate dateAfter, LocalDate dateBefore) {
        // If both dates are null, that's valid (no selection)
        if (dateAfter == null && dateBefore == null) {
            return true;
        }

        // If only one date is provided, check if it satisfies the min/max constraints
        if (dateAfter != null && dateBefore == null) {
            return (minDate == null || !dateAfter.isBefore(minDate)) &&
                   (maxDate == null || !dateAfter.isAfter(maxDate));
        }

        if (dateAfter == null && dateBefore != null) {
            return (minDate == null || !dateBefore.isBefore(minDate)) &&
                   (maxDate == null || !dateBefore.isAfter(maxDate));
        }

        // If both dates are provided, check if they form a valid range
        return (minDate == null || !dateAfter.isBefore(minDate)) &&
               (maxDate == null || !dateBefore.isAfter(maxDate)) &&
               !dateAfter.isAfter(dateBefore);
    }

    /**
     * Gets the result of the date selection.
     *
     * @return The date selector result
     */
    public DateSelectorResult getResult() {
        return result;
    }

    /**
     * Enum representing the completion type of the date selection.
     */
    public enum CompletionType {
        /** User confirmed the selection by clicking OK. */
        OK,
        /** User cancelled the selection by closing the dialog. */
        CANCEL
    }

    /**
     * Result record for the date selector.
     * Contains the selected dates, validation status, and completion type.
     */
    public record DateSelectorResult(
        LocalDate dateAfter,
        LocalDate dateBefore,
        boolean validated,
        CompletionType completionType
    ) {
        /**
         * Checks if the dialog was completed by clicking OK.
         *
         * @return true if the dialog was completed by clicking OK, false otherwise
         */
        public boolean isOk() {
            return completionType == CompletionType.OK;
        }

        /**
         * Checks if the dialog was cancelled.
         *
         * @return true if the dialog was cancelled, false otherwise
         */
        public boolean isCancelled() {
            return completionType == CompletionType.CANCEL;
        }

        /**
         * Gets the "date after" value.
         *
         * @return The "date after" value, or null if not set
         */
        public LocalDate getDateAfter() {
            return dateAfter;
        }

        /**
         * Gets the "date before" value.
         *
         * @return The "date before" value, or null if not set
         */
        public LocalDate getDateBefore() {
            return dateBefore;
        }

        /**
         * Checks if the dates are valid.
         *
         * @return true if the dates are valid, false otherwise
         */
        public boolean isValidated() {
            return validated;
        }

        /**
         * Gets the completion type.
         *
         * @return The completion type (OK or CANCEL)
         */
        public CompletionType getCompletionType() {
            return completionType;
        }
    }

    /**
     * Builder class for configuring and creating a date selector dialog.
     * This class follows the builder pattern to provide a fluent API for
     * configuring the date selector before showing it.
     */
    public static class Builder {
        /** Logger for this class. */
        private static final Logger logger = LoggerFactory.getLogger(Builder.class);

        /** The title of the dialog. */
        private String title;

        /** The initial "date after" value. */
        private LocalDate initialDateAfter;

        /** The initial "date before" value. */
        private LocalDate initialDateBefore;

        /** The minimum allowed date. */
        private LocalDate minDate;

        /** The maximum allowed date. */
        private LocalDate maxDate;

        /** The owner window of the dialog. */
        private Window owner;

        /** The locale for the dialog. */
        private Locale locale;

        /**
         * Creates a new builder instance.
         */
        public Builder() {
            // Default values - use localized title
            this.title = com.depavlo.gitlaberfx.util.I18nUtil.getMessage("date.selector.title");
            // Always use the current locale from I18nUtil
            this.locale = com.depavlo.gitlaberfx.util.I18nUtil.getCurrentLocale();
        }

        /**
         * Sets the title of the dialog.
         * Note: It's recommended to use the localized title from the resource bundle instead.
         *
         * @param title The title to set
         * @return This builder instance for method chaining
         */
        public Builder title(String title) {
            String localizedTitle = com.depavlo.gitlaberfx.util.I18nUtil.getMessage("date.selector.title");
            if (!title.equals(localizedTitle)) {
                logger.warn("Custom title '{}' provided. Consider using the localized title '{}' from the resource bundle instead.", 
                           title, localizedTitle);
            }
            this.title = title;
            return this;
        }

        /**
         * Sets the initial "date after" value.
         *
         * @param date The date to set
         * @return This builder instance for method chaining
         */
        public Builder dateAfter(LocalDate date) {
            this.initialDateAfter = date;
            return this;
        }

        /**
         * Sets the initial "date before" value.
         *
         * @param date The date to set
         * @return This builder instance for method chaining
         */
        public Builder dateBefore(LocalDate date) {
            this.initialDateBefore = date;
            return this;
        }

        /**
         * Sets the minimum allowed date.
         *
         * @param date The minimum date
         * @return This builder instance for method chaining
         */
        public Builder minDate(LocalDate date) {
            this.minDate = date;
            return this;
        }

        /**
         * Sets the maximum allowed date.
         *
         * @param date The maximum date
         * @return This builder instance for method chaining
         */
        public Builder maxDate(LocalDate date) {
            this.maxDate = date;
            return this;
        }

        /**
         * Sets the owner window of the dialog.
         *
         * @param owner The owner window
         * @return This builder instance for method chaining
         */
        public Builder owner(Window owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Sets the locale for the dialog.
         * Note: This method is deprecated. The dialog will always use the current locale from I18nUtil.
         *
         * @param locale The locale to use (ignored, current locale from I18nUtil is used instead)
         * @return This builder instance for method chaining
         */
        public Builder locale(Locale locale) {
            // Always use the current locale from I18nUtil, regardless of what's passed in
            Locale currentLocale = com.depavlo.gitlaberfx.util.I18nUtil.getCurrentLocale();
            if (!locale.equals(currentLocale)) {
                logger.warn("Ignoring provided locale {}. Using current locale {} from I18nUtil instead.", 
                           locale, currentLocale);
            }
            this.locale = currentLocale;
            return this;
        }

        /**
         * Builds and shows the date selector dialog.
         *
         * @return The result of the date selection, or null if an error occurred
         */
        public DateSelectorResult build() {
            try {
                // Always use the current locale from I18nUtil
                Locale currentLocale = com.depavlo.gitlaberfx.util.I18nUtil.getCurrentLocale();
                // Load the FXML file
                ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", currentLocale);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/date-selector.fxml"), bundle);
                Parent root = loader.load();

                // Get the controller
                DateSelectorController controller = loader.getController();

                // Create the stage
                Stage stage = new Stage();
                // Always use the localized title from the resource bundle
                stage.setTitle(com.depavlo.gitlaberfx.util.I18nUtil.getMessage("date.selector.title"));
                stage.setScene(new Scene(root));
                stage.setResizable(true);

                // Set modality and owner
                stage.initModality(Modality.APPLICATION_MODAL);
                if (owner != null) {
                    stage.initOwner(owner);
                }

                // Initialize the controller
                controller.initialize(stage, initialDateAfter, initialDateBefore, minDate, maxDate);

                // Show the dialog and wait for it to close
                stage.showAndWait();

                // Return the result
                return controller.getResult();
            } catch (IOException e) {
                logger.error("Error loading date selector dialog", e);
                return null;
            }
        }

        /**
         * Static factory method to create a new builder instance.
         *
         * @return A new builder instance
         */
        public static Builder create() {
            return new Builder();
        }
    }
}
