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

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.controller.AboutController;
import com.depavlo.gitlaberfx.controller.DatePickerController;
import com.depavlo.gitlaberfx.controller.DeleteConfirmationController;
import com.depavlo.gitlaberfx.controller.SettingsController;
import com.depavlo.gitlaberfx.model.BranchModel;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.depavlo.gitlaberfx.util.I18nUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class DialogHelper {
    private static final Logger logger = LoggerFactory.getLogger(DialogHelper.class);
    private static Stage loadingStage;

    public static class SettingsResult {
        private final boolean saved;
        private final boolean localeChanged;

        public SettingsResult(boolean saved, boolean localeChanged) {
            this.saved = saved;
            this.localeChanged = localeChanged;
        }

        public boolean isSaved() {
            return saved;
        }

        public boolean isLocaleChanged() {
            return localeChanged;
        }
    }

    /**
     * Shows a loading dialog with a message.
     * 
     * @param parentStage The parent stage
     * @param message The message to display
     * @return The loading dialog stage
     */
    public static Stage showLoadingDialog(Stage parentStage, String message) {
        if (loadingStage != null && loadingStage.isShowing()) {
            hideLoadingDialog();
        }

        try {
            // Create a progress indicator
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

            // Create a label with the message
            Label messageLabel = new Label(message);
            messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            // Create a VBox to hold the progress indicator and message
            VBox vbox = new VBox(10);
            vbox.setAlignment(Pos.CENTER);
            vbox.getChildren().addAll(progressIndicator, messageLabel);
            vbox.setPrefSize(300, 150);
            vbox.setStyle("-fx-background-color: white; -fx-padding: 20px;");

            // Create a scene with the VBox
            Scene scene = new Scene(vbox);

            // Create a stage for the loading dialog
            loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.UNDECORATED);
            loadingStage.initModality(Modality.APPLICATION_MODAL);
            loadingStage.initOwner(parentStage);
            loadingStage.setScene(scene);

            // If we're not on the JavaFX application thread, use Platform.runLater
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(() -> {
                    loadingStage.show();
                    // Center the dialog on the parent stage
                    loadingStage.setX(parentStage.getX() + (parentStage.getWidth() - loadingStage.getWidth()) / 2);
                    loadingStage.setY(parentStage.getY() + (parentStage.getHeight() - loadingStage.getHeight()) / 2);
                });
            } else {
                // We're already on the JavaFX application thread, so show directly
                loadingStage.show();
                // Center the dialog on the parent stage
                loadingStage.setX(parentStage.getX() + (parentStage.getWidth() - loadingStage.getWidth()) / 2);
                loadingStage.setY(parentStage.getY() + (parentStage.getHeight() - loadingStage.getHeight()) / 2);
            }

            return loadingStage;
        } catch (Exception e) {
            logger.error("Error showing loading dialog", e);
            return null;
        }
    }

    /**
     * Hides the loading dialog.
     */
    public static void hideLoadingDialog() {
        if (loadingStage != null) {
            // If we're not on the JavaFX application thread, use Platform.runLater
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(() -> {
                    if (loadingStage != null) {
                        loadingStage.close();
                        loadingStage = null;
                    }
                });
            } else {
                // We're already on the JavaFX application thread, so close directly
                loadingStage.close();
                loadingStage = null;
            }
        }
    }

    public static SettingsResult showSettingsDialog(Stage parentStage, AppConfig config) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/settings.fxml"));
            loader.setResources(ResourceBundle.getBundle("i18n.messages", I18nUtil.getCurrentLocale()));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle(I18nUtil.getMessage("dialog.settings"));
            stage.setScene(scene);

            SettingsController controller = loader.getController();
            controller.initialize(config, stage);

            stage.showAndWait();
            return new SettingsResult(controller.isSaved(), controller.isLocaleChanged());
        } catch (IOException e) {
            logger.error("Error showing settings dialog", e);
            return new SettingsResult(false, false);
        }
    }

    public static List<BranchModel> showDeleteConfirmationDialog(Stage parentStage, List<BranchModel> branches) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/delete-confirmation.fxml"));
            loader.setResources(ResourceBundle.getBundle("i18n.messages", I18nUtil.getCurrentLocale()));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle(I18nUtil.getMessage("dialog.delete.confirmation"));
            stage.setScene(scene);

            DeleteConfirmationController controller = loader.getController();
            controller.initialize(branches, stage);

            stage.showAndWait();
            return controller.getSelectedBranches();
        } catch (IOException e) {
            logger.error("Error showing delete confirmation dialog", e);
            return null;
        }
    }

    public static LocalDate showDatePickerDialog(Stage parentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/date-picker.fxml"));
            loader.setResources(ResourceBundle.getBundle("i18n.messages", I18nUtil.getCurrentLocale()));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle(I18nUtil.getMessage("dialog.date.picker"));
            stage.setScene(scene);

            DatePickerController controller = loader.getController();
            controller.initialize(stage);

            stage.showAndWait();
            return controller.getSelectedDate();
        } catch (IOException e) {
            logger.error("Error showing date picker dialog", e);
            return null;
        }
    }

    public static void showAboutDialog(Stage parentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/about.fxml"));
            loader.setResources(ResourceBundle.getBundle("i18n.messages", I18nUtil.getCurrentLocale()));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle(I18nUtil.getMessage("dialog.about"));
            stage.setScene(scene);

            AboutController controller = loader.getController();
            controller.initialize(stage);

            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Error showing about dialog", e);
        }
    }
} 
