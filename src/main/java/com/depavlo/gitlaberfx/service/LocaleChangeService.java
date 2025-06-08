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
 * This class provides functionality to change the application's locale at runtime,
 * preserving the current state of the UI and reloading the interface with the new locale.
 * It handles saving the current UI state, changing the locale, and restoring the state
 * after the UI has been reloaded with the new locale.
 */
public class LocaleChangeService {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(LocaleChangeService.class);

    /**
     * Inner class to store the UI state that needs to be preserved during locale change.
     * This includes selected project and branch information that should be restored
     * after the UI is reloaded with the new locale.
     */
    public static class SavedState {
        /** The ID of the currently selected project. */
        public String projectId;

        /** The name of the currently selected project. */
        public String projectName;

        /** The name of the currently selected target branch. */
        public String targetBranchName;
    }

    /**
     * Changes the application's locale while preserving the current UI state.
     * This method saves the current UI state, changes the locale, reloads the UI,
     * and then restores the saved state in the new UI.
     *
     * @param newLocale The new locale to set
     * @param config The application configuration to update with the new locale
     * @param stage The primary stage of the application
     * @param currentController The current main controller containing the UI state to preserve
     * @throws RuntimeException if there's an error during the locale change process
     */
    public static void changeLocale(Locale newLocale, AppConfig config, Stage stage, MainController currentController) {
        logger.info("Changing locale to: {}", newLocale);
        try {
            // Get the entire state model from the current controller
            UIStateModel existingModel = currentController.getUiStateModel();
            SavedState savedSelections = saveState(existingModel);

            I18nUtil.setLocale(newLocale);
            config.setLocale(newLocale.toLanguageTag().replace('-', '_'));
            config.save();

            MainController newController = reloadUI(stage, config);

            // Pass both the existing model data and the specific selections to the new controller
            newController.repopulateFromState(existingModel, savedSelections);

        } catch (Exception e) {
            logger.error("Error changing locale", e);
            throw new RuntimeException("Failed to change locale", e);
        }
    }

    /**
     * Saves the current UI state from the model into a SavedState object.
     * This method extracts the essential state information that needs to be preserved
     * during the locale change and UI reload.
     *
     * @param model The UI state model containing the current state
     * @return A SavedState object containing the essential state information
     */
    private static SavedState saveState(UIStateModel model) {
        logger.debug("Saving UI state from model");
        SavedState state = new SavedState();
        state.projectId = model.getCurrentProjectId();
        state.projectName = model.getCurrentProjectName();
        state.targetBranchName = model.getCurrentTargetBranchName();
        logger.debug("Saved state: Project='{}', TargetBranch='{}'", state.projectName, state.targetBranchName);
        return state;
    }

    /**
     * Reloads the UI with the new locale.
     * This method creates a new scene with the updated locale resources,
     * initializes a new MainController, and sets it on the primary stage.
     *
     * @param stage The primary stage of the application
     * @param config The application configuration
     * @return The newly created and initialized MainController
     * @throws IOException if there's an error loading the FXML or initializing the controller
     */
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
