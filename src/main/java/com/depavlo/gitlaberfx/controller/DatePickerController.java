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
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Controller for the date picker dialog.
 * This class handles the functionality of the date picker dialog, which allows
 * the user to select a date from a calendar.
 */
public class DatePickerController {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(DatePickerController.class);

    /** The date picker control that allows the user to select a date. */
    @FXML
    private DatePicker datePicker;

    /** The stage that contains the date picker dialog. */
    private Stage stage;

    /** The date selected by the user, or null if no date was selected. */
    private LocalDate selectedDate;

    /**
     * Initializes the controller with the stage that contains the date picker dialog.
     * This method is called after the FXML has been loaded.
     * It sets the initial date to the current date.
     *
     * @param stage The stage that contains the date picker dialog
     */
    public void initialize(Stage stage) {
        logger.debug("initialize: stage={}", stage != null ? "not null" : "null");
        this.stage = stage;
        datePicker.setValue(LocalDate.now());
    }

    /**
     * Confirms the date selection and closes the dialog.
     * This method is called when the user clicks the confirm button.
     * It saves the selected date and closes the dialog.
     */
    @FXML
    private void confirm() {
        logger.debug("Confirming date selection");
        selectedDate = datePicker.getValue();
        stage.close();
    }

    /**
     * Cancels the date selection and closes the dialog.
     * This method is called when the user clicks the cancel button.
     * It sets the selected date to null and closes the dialog.
     */
    @FXML
    private void cancel() {
        logger.debug("Cancelling date selection");
        selectedDate = null;
        stage.close();
    }

    /**
     * Gets the date selected by the user.
     *
     * @return The selected date, or null if no date was selected or the selection was cancelled
     */
    public LocalDate getSelectedDate() {
        logger.debug("getSelectedDate: selectedDate={}", selectedDate);
        return selectedDate;
    }
} 
