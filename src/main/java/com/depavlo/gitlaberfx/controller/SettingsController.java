package com.depavlo.gitlaberfx.controller;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.service.GitLabService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML
    private TextField gitlabUrlField;
    
    @FXML
    private PasswordField apiKeyField;
    
    @FXML
    private TextField usernameField;

    private AppConfig config;
    private Stage stage;
    private boolean saved = false;

    public void initialize(AppConfig config, Stage stage) {
        this.config = config;
        this.stage = stage;

        gitlabUrlField.setText(config.getGitlabUrl());
        apiKeyField.setText(config.getApiKey());
        usernameField.setText(config.getUsername());
    }

    @FXML
    private void testConnection() {
        try {
            AppConfig testConfig = new AppConfig();
            testConfig.setGitlabUrl(gitlabUrlField.getText());
            testConfig.setApiKey(apiKeyField.getText());
            testConfig.setUsername(usernameField.getText());

            GitLabService service = new GitLabService(testConfig);
            service.connect();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Успіх");
            alert.setHeaderText(null);
            alert.setContentText("З'єднання успішно встановлено!");
            alert.showAndWait();
        } catch (IOException e) {
            logger.error("Connection test failed", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Помилка");
            alert.setHeaderText(null);
            alert.setContentText("Помилка з'єднання: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void save() {
        config.setGitlabUrl(gitlabUrlField.getText());
        config.setApiKey(apiKeyField.getText());
        config.setUsername(usernameField.getText());
        config.save();
        saved = true;
        stage.close();
    }

    @FXML
    private void cancel() {
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }
} 