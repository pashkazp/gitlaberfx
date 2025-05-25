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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class DialogHelper {
    private static final Logger logger = LoggerFactory.getLogger(DialogHelper.class);
    private static Stage loadingStage;

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

            // Show the loading dialog
            Platform.runLater(() -> loadingStage.show());

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
            Platform.runLater(() -> {
                loadingStage.close();
                loadingStage = null;
            });
        }
    }

    public static boolean showSettingsDialog(Stage parentStage, AppConfig config) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/settings.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle("Налаштування");
            stage.setScene(scene);

            SettingsController controller = loader.getController();
            controller.initialize(config, stage);

            stage.showAndWait();
            return controller.isSaved();
        } catch (IOException e) {
            logger.error("Error showing settings dialog", e);
            return false;
        }
    }

    public static List<BranchModel> showDeleteConfirmationDialog(Stage parentStage, List<BranchModel> branches) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/delete-confirmation.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle("Підтвердження видалення");
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
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle("Вибір дати");
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
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle("Про програму");
            stage.setScene(scene);

            AboutController controller = loader.getController();
            controller.initialize(stage);

            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Error showing about dialog", e);
        }
    }
} 
