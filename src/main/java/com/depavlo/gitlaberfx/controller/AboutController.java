package com.depavlo.gitlaberfx.controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AboutController {
    private static final Logger logger = LoggerFactory.getLogger(AboutController.class);

    private Stage stage;

    public void initialize(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void close() {
        logger.debug("Closing about dialog");
        stage.close();
    }
} 