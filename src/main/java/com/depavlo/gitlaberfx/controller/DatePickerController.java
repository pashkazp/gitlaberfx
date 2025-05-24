package com.depavlo.gitlaberfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class DatePickerController {
    private static final Logger logger = LoggerFactory.getLogger(DatePickerController.class);

    @FXML
    private DatePicker datePicker;

    private Stage stage;
    private LocalDate selectedDate;

    public void initialize(Stage stage) {
        this.stage = stage;
        datePicker.setValue(LocalDate.now());
    }

    @FXML
    private void confirm() {
        logger.debug("Confirming date selection");
        selectedDate = datePicker.getValue();
        stage.close();
    }

    @FXML
    private void cancel() {
        logger.debug("Cancelling date selection");
        selectedDate = null;
        stage.close();
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }
} 