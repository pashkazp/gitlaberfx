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
