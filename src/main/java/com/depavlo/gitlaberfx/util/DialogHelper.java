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
import com.depavlo.gitlaberfx.model.OperationConfirmationResult;
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

/**
 * Utility class for creating and managing various dialogs in the application.
 * This class provides methods for showing loading indicators, settings dialogs,
 * confirmation dialogs, date pickers, and about dialogs.
 * All methods are static and can be called from any part of the application.
 */
public class DialogHelper {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(DialogHelper.class);

    /** Reference to the currently displayed loading dialog stage. */
    private static Stage loadingStage;

    /**
     * Shows a loading dialog with a message.
     * 
     * @param parentStage The parent stage
     * @param message The message to display
     * @return The loading dialog stage
     */
    public static Stage showLoadingDialog(Stage parentStage, String message) {
        logger.debug("showLoadingDialog: parentStage={}, message={}", parentStage != null ? "not null" : "null", message);
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
        logger.debug("hideLoadingDialog");
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

    /**
     * Shows the settings dialog.
     * 
     * @param parentStage The parent stage
     * @param config The application configuration
     * @param mainController The main controller (can be null if not available)
     * @return true if settings were saved, false otherwise
     */
    public static boolean showSettingsDialog(Stage parentStage, AppConfig config, com.depavlo.gitlaberfx.controller.MainController mainController) {
        logger.debug("showSettingsDialog: parentStage={}, config={}, mainController={}", 
                parentStage != null ? "not null" : "null", 
                config != null ? "not null" : "null", 
                mainController != null ? "not null" : "null");
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

            // Set the main controller reference if provided
            if (mainController != null) {
                controller.setMainController(mainController);
            }

            stage.showAndWait();
            return controller.isSaved();
        } catch (IOException e) {
            logger.error("Error showing settings dialog", e);
            return false;
        }
    }

    /**
     * Shows the settings dialog (overloaded method for backward compatibility).
     * 
     * @param parentStage The parent stage
     * @param config The application configuration
     * @return true if settings were saved, false otherwise
     */
    public static boolean showSettingsDialog(Stage parentStage, AppConfig config) {
        logger.debug("showSettingsDialog: parentStage={}, config={}", 
                parentStage != null ? "not null" : "null", 
                config != null ? "not null" : "null");
        return showSettingsDialog(parentStage, config, null);
    }

    /**
     * Shows a confirmation dialog for deleting or archiving branches (backward compatibility method).
     * This method is provided for backward compatibility with code that doesn't provide
     * deletion type and project name.
     * 
     * @param parentStage The parent stage
     * @param branches The list of branches to potentially delete or archive
     * @return The OperationConfirmationResult containing the list of branches selected and whether to archive them,
     *         or null if the dialog was cancelled or an error occurred
     */
    public static OperationConfirmationResult showDeleteConfirmationDialog(Stage parentStage, List<BranchModel> branches) {
        return showDeleteConfirmationDialog(parentStage, branches, 
                                          I18nUtil.getMessage("main.delete.selected"), 
                                          "");
    }

    /**
     * Shows a confirmation dialog for deleting or archiving branches.
     * This dialog displays a list of branches and allows the user to confirm which ones to delete or archive.
     * This version of the method includes information about the operation type and project name.
     * 
     * @param parentStage The parent stage
     * @param branches The list of branches to potentially delete or archive
     * @param deletionType The type of operation (e.g., "Deleting selected branches")
     * @param projectName The name of the project from which branches will be processed
     * @return The OperationConfirmationResult containing the list of branches selected and whether to archive them,
     *         or null if the dialog was cancelled or an error occurred
     */
    public static OperationConfirmationResult showDeleteConfirmationDialog(Stage parentStage, List<BranchModel> branches, 
                                                                String deletionType, String projectName) {
        logger.debug("showDeleteConfirmationDialog: parentStage={}, branches.size={}, deletionType={}, projectName={}", 
                parentStage != null ? "not null" : "null", 
                branches != null ? branches.size() : "null",
                deletionType,
                projectName);
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/delete-confirmation.fxml"));
            loader.setResources(ResourceBundle.getBundle("i18n.messages", I18nUtil.getCurrentLocale()));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle(I18nUtil.getMessage("dialog.delete.confirmation"));
            stage.setScene(scene);

            // Create deep copies of branches to avoid modifying the original list
            List<BranchModel> branchCopies = branches.stream()
                    .map(BranchModel::new)  // Use copy constructor
                    .toList();

            DeleteConfirmationController controller = loader.getController();
            controller.initialize(branchCopies, stage, deletionType, projectName);

            stage.showAndWait();
            return controller.getSelectedBranches();
        } catch (IOException e) {
            logger.error("Error showing delete confirmation dialog", e);
            return null;
        }
    }

    /**
     * Shows a date picker dialog.
     * This dialog allows the user to select a date from a calendar.
     * 
     * @param parentStage The parent stage
     * @return The selected date, or null if no date was selected or an error occurred
     */
    public static LocalDate showDatePickerDialog(Stage parentStage) {
        logger.debug("showDatePickerDialog: parentStage={}", parentStage != null ? "not null" : "null");
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

    /**
     * Shows the about dialog.
     * This dialog displays information about the application, such as version, author, and license.
     * 
     * @param parentStage The parent stage
     */
    public static void showAboutDialog(Stage parentStage) {
        logger.debug("showAboutDialog: parentStage={}", parentStage != null ? "not null" : "null");
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
