package com.depavlo.gitlaberfx.util;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.controller.AboutController;
import com.depavlo.gitlaberfx.controller.DatePickerController;
import com.depavlo.gitlaberfx.controller.DeleteConfirmationController;
import com.depavlo.gitlaberfx.controller.ProgressDialogController;
import com.depavlo.gitlaberfx.controller.SettingsController;
import com.depavlo.gitlaberfx.model.BranchModel;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class DialogHelper {
    private static final Logger logger = LoggerFactory.getLogger(DialogHelper.class);

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

    /**
     * Shows a progress dialog for a long-running task.
     * 
     * @param parentStage the parent stage
     * @param task the task to execute
     * @param title the dialog title
     * @param message the message to display
     * @return the progress dialog controller
     */
    public static ProgressDialogController showProgressDialog(Stage parentStage, Task<?> task, String title, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/fxml/progress-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(false);

            ProgressDialogController controller = loader.getController();
            controller.initialize(stage, task, message);

            // Show the dialog but don't wait for it to close
            stage.show();

            return controller;
        } catch (IOException e) {
            logger.error("Error showing progress dialog", e);
            return null;
        }
    }
}
