package com.depavlo.gitlaberfx.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProgressDialogController {
    private static final Logger logger = LoggerFactory.getLogger(ProgressDialogController.class);

    @FXML
    private Label messageLabel;

    @FXML
    private Label detailsLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button cancelButton;

    @FXML
    private Button forceStopButton;

    private Stage stage;
    private Task<?> task;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final AtomicBoolean forceStopRequested = new AtomicBoolean(false);

    public void initialize(Stage stage, Task<?> task, String message) {
        this.stage = stage;
        this.task = task;
        
        messageLabel.setText(message);
        
        // Bind progress bar to task progress
        progressBar.progressProperty().bind(task.progressProperty());
        
        // Bind details label to task message
        detailsLabel.textProperty().bind(task.messageProperty());
        
        // Close dialog when task is done
        task.setOnSucceeded(event -> stage.close());
        task.setOnFailed(event -> {
            logger.error("Task failed", task.getException());
            stage.close();
        });
        task.setOnCancelled(event -> stage.close());
    }

    @FXML
    private void handleCancel() {
        logger.debug("Cancel requested");
        cancelRequested.set(true);
        cancelButton.setDisable(true);
        cancelButton.setText("Очікування завершення...");
    }

    @FXML
    private void handleForceStop() {
        logger.debug("Force stop requested");
        forceStopRequested.set(true);
        if (task != null) {
            task.cancel(true);
        }
        stage.close();
    }

    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    public boolean isForceStopRequested() {
        return forceStopRequested.get();
    }

    public void setProgress(double progress) {
        if (progress >= 0) {
            progressBar.setProgress(progress);
        }
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setDetails(String details) {
        detailsLabel.setText(details);
    }
}