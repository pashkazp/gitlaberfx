package com.depavlo.gitlaberfx.util;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.controller.AboutController;
import com.depavlo.gitlaberfx.controller.DatePickerController;
import com.depavlo.gitlaberfx.controller.DeleteConfirmationController;
import com.depavlo.gitlaberfx.controller.SettingsController;
import com.depavlo.gitlaberfx.model.BranchModel;
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
} 