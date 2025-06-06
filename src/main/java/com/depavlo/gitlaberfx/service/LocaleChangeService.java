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
import java.util.concurrent.CompletableFuture;

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
            SavedState state = saveState(currentController.getUiStateModel());

            I18nUtil.setLocale(newLocale);
            config.setLocale(newLocale.toLanguageTag().replace('-', '_'));
            config.save();

            MainController newController = reloadUI(stage, config);
            restoreState(newController, state);

        } catch (Exception e) {
            logger.error("Error changing locale", e);
            throw new RuntimeException("Failed to change locale", e);
        }
    }

    private static SavedState saveState(UIStateModel model) {
        logger.debug("Saving UI state from model");
        SavedState state = new SavedState();
        // Corrected method calls to use standard getters
        state.projectName = model.getCurrentProjectName();
        state.targetBranchName = model.getCurrentTargetBranchName();
        logger.debug("Saved state: Project='{}', TargetBranch='{}'", state.projectName, state.targetBranchName);
        return state;
    }

    private static void restoreState(MainController controller, SavedState state) {
        logger.debug("Orchestrating state restoration for project: '{}', target: '{}'", state.projectName, state.targetBranchName);

        CompletableFuture<Void> projectsReadyFuture = controller.startInitialLoad();

        projectsReadyFuture.thenAcceptAsync(v -> {
            if (state.projectName != null && !state.projectName.isEmpty()) {
                if (controller.getProjectComboBox().getItems().contains(state.projectName)) {
                    logger.debug("Projects loaded. Programmatically setting project to '{}'", state.projectName);
                    controller.getProjectComboBox().setValue(state.projectName);

                    controller.getBranchLoadFuture().thenAcceptAsync(v2 -> {
                        if (state.targetBranchName != null && !state.targetBranchName.isEmpty()) {
                            if (controller.getDestBranchComboBox().getItems().contains(state.targetBranchName)) {
                                logger.debug("Branches loaded. Programmatically setting target branch to '{}'", state.targetBranchName);
                                controller.getDestBranchComboBox().setValue(state.targetBranchName);
                            } else {
                                logger.warn("Saved target branch '{}' not found in the list for project '{}'", state.targetBranchName, state.projectName);
                            }
                        }
                    }, Platform::runLater);
                } else {
                    logger.warn("Saved project '{}' not found in the reloaded list.", state.projectName);
                }
            }
        }, Platform::runLater);
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
        controller.initialize(config, stage);
        return controller;
    }
}
