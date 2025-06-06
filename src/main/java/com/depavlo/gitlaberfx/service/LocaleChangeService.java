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
package com.depavlo.gitlaberfx.service;

import com.depavlo.gitlaberfx.GitlaberApp;
import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.controller.MainController;
import com.depavlo.gitlaberfx.model.UIStateModel;
import com.depavlo.gitlaberfx.util.I18nUtil;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Service for handling dynamic locale changes in the application.
 */
public class LocaleChangeService {
    private static final Logger logger = LoggerFactory.getLogger(LocaleChangeService.class);

    private static class SavedState {
        String projectName;
        String targetBranchName;
    }

    public static void changeLocale(Locale newLocale, AppConfig config, Stage stage, MainController currentController) {
        logger.info("Changing locale to: {}", newLocale);

        try {
            // 1. Save current state from the model
            SavedState state = saveState(currentController.getUiStateModel());

            // 2. Update locale in config and util
            I18nUtil.setLocale(newLocale);
            config.setLocale(newLocale.toLanguageTag().replace('-', '_')); // Store as uk_UA
            config.save();

            // 3. Reload UI
            MainController newController = reloadUI(stage, config);

            // 4. Restore state by re-triggering the application logic
            restoreState(newController, state);

        } catch (Exception e) {
            logger.error("Error changing locale", e);
            throw new RuntimeException("Failed to change locale", e);
        }
    }

    private static SavedState saveState(UIStateModel model) {
        logger.debug("Saving UI state from model");
        SavedState state = new SavedState();
        state.projectName = model.getCurrentProjectNameProperty().get();
        state.targetBranchName = model.getCurrentTargetBranchNameProperty().get();
        logger.debug("Saved state: Project='{}', TargetBranch='{}'", state.projectName, state.targetBranchName);
        return state;
    }

    private static void restoreState(MainController controller, SavedState state) {
        logger.debug("Attempting to restore UI state: Project='{}'", state.projectName);
        if (state.projectName != null && !state.projectName.isEmpty()) {
            // We can't set value directly as items are loaded asynchronously.
            // We add a listener that will set the value once the project list is populated.
            controller.getProjectComboBox().getItems().addListener((javafx.collections.ListChangeListener.Change<? extends String> c) -> {
                logger.debug("Project list updated in new controller. Attempting to select '{}'", state.projectName);
                if (controller.getProjectComboBox().getItems().contains(state.projectName)) {
                    Platform.runLater(() -> {
                        controller.getProjectComboBox().setValue(state.projectName);
                        logger.debug("Successfully set project to '{}'. Now restoring target branch.", state.projectName);
                        restoreTargetBranch(controller, state);
                    });
                }
            });
        }
    }

    private static void restoreTargetBranch(MainController controller, SavedState state) {
        if (state.targetBranchName != null && !state.targetBranchName.isEmpty()) {
            // Similar to projects, we wait for the branch list to be populated.
            controller.getDestBranchComboBox().getItems().addListener((javafx.collections.ListChangeListener.Change<? extends String> c) -> {
                logger.debug("Branch list updated for project '{}'. Attempting to select target branch '{}'", state.projectName, state.targetBranchName);
                if (controller.getDestBranchComboBox().getItems().contains(state.targetBranchName)) {
                    Platform.runLater(() -> {
                        controller.getDestBranchComboBox().setValue(state.targetBranchName);
                        logger.debug("Successfully set target branch to '{}'", state.targetBranchName);
                    });
                }
            });
        }
    }

    private static MainController reloadUI(Stage stage, AppConfig config) throws IOException {
        logger.info("Reloading UI with new locale");
        FXMLLoader fxmlLoader = new FXMLLoader(GitlaberApp.class.getResource("/fxml/main.fxml"));
        fxmlLoader.setResources(ResourceBundle.getBundle("i18n.messages", I18nUtil.getCurrentLocale()));

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

        stage.setTitle(I18nUtil.getMessage("app.title"));
        stage.setScene(scene);

        MainController controller = fxmlLoader.getController();
        // The initialize method will be called automatically by FXMLLoader
        // but we pass dependencies manually after it's loaded.
        controller.initialize(config, stage);
        return controller;
    }
}