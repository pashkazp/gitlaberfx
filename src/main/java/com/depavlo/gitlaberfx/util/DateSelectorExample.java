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
package com.depavlo.gitlaberfx.util;

import com.depavlo.gitlaberfx.controller.DateSelectorController;
import com.depavlo.gitlaberfx.controller.DateSelectorController.DateSelectorResult;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Example class demonstrating how to use the DateSelectorBuilder.
 * This class provides static methods that show how to create and use
 * the date selector in different scenarios.
 */
public class DateSelectorExample {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(DateSelectorExample.class);

    /**
     * Shows a date selector with default settings.
     *
     * @param owner The owner window of the dialog
     * @return The result of the date selection, or null if an error occurred
     */
    public static DateSelectorResult showDefaultDateSelector(Window owner) {
        logger.debug("Showing default date selector");

        // Create a date selector with default settings
        DateSelectorResult result = DateSelectorController.Builder.create()
                .title("Date Selector")
                .owner(owner)
                .build();

        // Log and return the result
        logger.debug("Date selector result: {}", result);
        return result;
    }

    /**
     * Shows a date selector with initial values.
     *
     * @param owner The owner window of the dialog
     * @param dateAfter The initial "date after" value
     * @param dateBefore The initial "date before" value
     * @return The result of the date selection, or null if an error occurred
     */
    public static DateSelectorResult showDateSelectorWithInitialValues(Window owner, LocalDate dateAfter, LocalDate dateBefore) {
        logger.debug("Showing date selector with initial values: dateAfter={}, dateBefore={}", dateAfter, dateBefore);

        // Create a date selector with initial values
        DateSelectorResult result = DateSelectorController.Builder.create()
                .title("Date Selector")
                .owner(owner)
                .dateAfter(dateAfter)
                .dateBefore(dateBefore)
                .build();

        // Log and return the result
        logger.debug("Date selector result: {}", result);
        return result;
    }

    /**
     * Shows a date selector with constraints.
     *
     * @param owner The owner window of the dialog
     * @param minDate The minimum allowed date
     * @param maxDate The maximum allowed date
     * @return The result of the date selection, or null if an error occurred
     */
    public static DateSelectorResult showDateSelectorWithConstraints(Window owner, LocalDate minDate, LocalDate maxDate) {
        logger.debug("Showing date selector with constraints: minDate={}, maxDate={}", minDate, maxDate);

        // Create a date selector with constraints
        DateSelectorResult result = DateSelectorController.Builder.create()
                .title("Date Selector")
                .owner(owner)
                .minDate(minDate)
                .maxDate(maxDate)
                .build();

        // Log and return the result
        logger.debug("Date selector result: {}", result);
        return result;
    }

    /**
     * Shows a date selector with localization.
     *
     * @param owner The owner window of the dialog
     * @param locale The locale to use
     * @return The result of the date selection, or null if an error occurred
     */
    public static DateSelectorResult showDateSelectorWithLocalization(Window owner, Locale locale) {
        logger.debug("Showing date selector with localization: locale={}", locale);

        // Create a date selector with localization
        DateSelectorResult result = DateSelectorController.Builder.create()
                .title("Date Selector")
                .owner(owner)
                .locale(locale)
                .build();

        // Log and return the result
        logger.debug("Date selector result: {}", result);
        return result;
    }

    /**
     * Processes the result of a date selection and shows an alert with the selected dates.
     *
     * @param result The result of the date selection
     * @param owner The owner window of the alert
     */
    public static void processDateSelectorResult(DateSelectorResult result, Window owner) {
        if (result == null) {
            logger.warn("Date selector returned null result");
            showAlert(AlertType.ERROR, "Error", "Failed to show date selector", owner);
            return;
        }

        if (result.isCancelled()) {
            logger.debug("Date selection was cancelled");
            showAlert(AlertType.INFORMATION, "Cancelled", "Date selection was cancelled", owner);
            return;
        }

        if (!result.isValidated()) {
            logger.warn("Date selection is not valid");
            showAlert(AlertType.WARNING, "Invalid Selection", "The selected dates are not valid", owner);
            return;
        }

        // Format the dates for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateAfterStr = result.getDateAfter() != null ? result.getDateAfter().format(formatter) : "not set";
        String dateBeforeStr = result.getDateBefore() != null ? result.getDateBefore().format(formatter) : "not set";

        // Show the result
        String message = String.format("Date after: %s\nDate before: %s", dateAfterStr, dateBeforeStr);
        showAlert(AlertType.INFORMATION, "Date Selection", message, owner);
    }

    /**
     * Shows an alert with the specified parameters.
     *
     * @param type The type of the alert
     * @param title The title of the alert
     * @param message The message of the alert
     * @param owner The owner window of the alert
     */
    private static void showAlert(AlertType type, String title, String message, Window owner) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }
}
