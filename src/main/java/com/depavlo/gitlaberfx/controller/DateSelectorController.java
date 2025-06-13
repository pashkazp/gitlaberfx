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

    /** The date picker control that allows the user to select a date. */
    @FXML
    private DatePicker datePicker;

    /** The text field for the "date after" value. */
    @FXML
    private TextField dateAfterField;

    /** The text field for the "date before" value. */
    @FXML
    private TextField dateBeforeField;

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

        // Set up the date picker
        datePicker.setValue(LocalDate.now());

        // Set up date constraints if provided
        if (minDate != null) {
            datePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(minDate) || 
                              (maxDate != null && date.isAfter(maxDate)));
                }
            });
        } else if (maxDate != null) {
            datePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isAfter(maxDate));
                }
            });
        }

        // Set initial values if provided
        if (initialDateAfter != null) {
            dateAfterField.setText(initialDateAfter.format(dateFormatter));
        }
        if (initialDateBefore != null) {
            dateBeforeField.setText(initialDateBefore.format(dateFormatter));
        }

        // Set up listeners
        setupListeners();

        // Set initial result
        result = new DateSelectorResult(null, null, true, CompletionType.CANCEL);
    }

    /**
     * Sets up listeners for the UI components.
     */
    private void setupListeners() {
        // When a date is selected in the date picker, update the active text field
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (dateAfterField.isFocused()) {
                    dateAfterField.setText(newVal.format(dateFormatter));
                    validateDates();
                } else if (dateBeforeField.isFocused()) {
                    dateBeforeField.setText(newVal.format(dateFormatter));
                    validateDates();
                } else {
                    // If no field has focus, update the field that was last focused or the first field
                    if (dateAfterField.getText().isEmpty()) {
                        dateAfterField.setText(newVal.format(dateFormatter));
                        dateAfterField.requestFocus();
                    } else {
                        dateBeforeField.setText(newVal.format(dateFormatter));
                        dateBeforeField.requestFocus();
                    }
                    validateDates();
                }
            }
        });

        // Add focus listeners to highlight the active field
        dateAfterField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                try {
                    LocalDate date = LocalDate.parse(dateAfterField.getText(), dateFormatter);
                    datePicker.setValue(date);
                } catch (DateTimeParseException e) {
                    // If the field doesn't contain a valid date, don't update the date picker
                }
            }
        });

        dateBeforeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                try {
                    LocalDate date = LocalDate.parse(dateBeforeField.getText(), dateFormatter);
                    datePicker.setValue(date);
                } catch (DateTimeParseException e) {
                    // If the field doesn't contain a valid date, don't update the date picker
                }
            }
        });

        // Add key listeners for validation
        dateAfterField.addEventHandler(KeyEvent.KEY_RELEASED, event -> validateDates());
        dateBeforeField.addEventHandler(KeyEvent.KEY_RELEASED, event -> validateDates());
    }

    /**
     * Validates the dates entered in the text fields.
     * This method checks if the dates are in a valid format and if they satisfy the constraints.
     * It also updates the visual feedback for the user.
     */
    private void validateDates() {
        boolean dateAfterValid = true;
        boolean dateBeforeValid = true;
        LocalDate dateAfter = null;
        LocalDate dateBefore = null;

        // Validate "date after" field
        if (!dateAfterField.getText().isEmpty()) {
            try {
                dateAfter = LocalDate.parse(dateAfterField.getText(), dateFormatter);
                if (minDate != null && dateAfter.isBefore(minDate)) {
                    dateAfterValid = false;
                }
                if (maxDate != null && dateAfter.isAfter(maxDate)) {
                    dateAfterValid = false;
                }
            } catch (DateTimeParseException e) {
                dateAfterValid = false;
            }
        }

        // Validate "date before" field
        if (!dateBeforeField.getText().isEmpty()) {
            try {
                dateBefore = LocalDate.parse(dateBeforeField.getText(), dateFormatter);
                if (minDate != null && dateBefore.isBefore(minDate)) {
                    dateBeforeValid = false;
                }
                if (maxDate != null && dateBefore.isAfter(maxDate)) {
                    dateBeforeValid = false;
                }
            } catch (DateTimeParseException e) {
                dateBeforeValid = false;
            }
        }

        // Check if "date after" is before "date before"
        if (dateAfterValid && dateBeforeValid && dateAfter != null && dateBefore != null) {
            if (dateAfter.isAfter(dateBefore)) {
                dateAfterValid = false;
                dateBeforeValid = false;
            }
        }

        // Update visual feedback
        updateFieldStyle(dateAfterField, dateAfterValid);
        updateFieldStyle(dateBeforeField, dateBeforeValid);
    }

    /**
     * Updates the style of a text field based on its validity.
     *
     * @param field The text field to update
     * @param valid Whether the field's content is valid
     */
    private void updateFieldStyle(TextField field, boolean valid) {
        if (valid) {
            field.setStyle("");
        } else {
            field.setStyle("-fx-text-fill: red;");
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

        // Parse the dates
        LocalDate dateAfter = parseDate(dateAfterField.getText());
        LocalDate dateBefore = parseDate(dateBeforeField.getText());

        // Validate the dates
        boolean valid = validateDateRange(dateAfter, dateBefore);

        // Create the result
        result = new DateSelectorResult(dateAfter, dateBefore, valid, CompletionType.OK);

        // Close the dialog
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
            // Default values
            this.title = "Date Selector";
            this.locale = Locale.getDefault();
        }

        /**
         * Sets the title of the dialog.
         *
         * @param title The title to set
         * @return This builder instance for method chaining
         */
        public Builder title(String title) {
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
         *
         * @param locale The locale to use
         * @return This builder instance for method chaining
         */
        public Builder locale(Locale locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Builds and shows the date selector dialog.
         *
         * @return The result of the date selection, or null if an error occurred
         */
        public DateSelectorResult build() {
            try {
                // Load the FXML file
                ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/date-selector.fxml"), bundle);
                Parent root = loader.load();

                // Get the controller
                DateSelectorController controller = loader.getController();

                // Create the stage
                Stage stage = new Stage();
                stage.setTitle(title);
                stage.setScene(new Scene(root));
                stage.setResizable(false);

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
