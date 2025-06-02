package com.depavlo.gitlaberfx.service;

import com.depavlo.gitlaberfx.GitlaberApp;
import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.controller.MainController;
import com.depavlo.gitlaberfx.model.BranchModel;
import com.depavlo.gitlaberfx.util.I18nUtil;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Service for handling dynamic locale changes in the application.
 * Provides functionality to change the locale and reload the UI without restarting the application.
 */
public class LocaleChangeService {
    private static final Logger logger = LoggerFactory.getLogger(LocaleChangeService.class);

    /**
     * Class to hold the UI state during locale change
     */
    private static class UIState {
        ObservableList<String> destBranchesItems;
        ObservableList<String> projectItems;
        int selectedProjectIndex = -1;
        int selectedDestBranchIndex = -1;
        ObservableList<BranchModel> branchModels;
        List<Integer> selectedBranchIndices;
        BranchModel scrollToItem = null;
        String currentProjectId;
    }

    /**
     * Changes the application locale and reloads the UI.
     * 
     * @param newLocale The new locale to set
     * @param config The application configuration
     * @param stage The main application stage
     * @param currentController The current MainController instance
     * @return The new MainController instance
     */
    public static MainController changeLocale(Locale newLocale, AppConfig config, Stage stage, MainController currentController) {
        logger.info("Changing locale to: {}", newLocale);

        try {
            // Step 1: Save the current UI state
            UIState uiState = saveUIState(currentController);

            // Update the locale in I18nUtil
            I18nUtil.setLocale(newLocale);

            // Update the locale in config
            config.setLocale(newLocale.getLanguage() + "_" + newLocale.getCountry());
            config.save();

            // Step 2: Reload the UI with the new locale
            MainController newController = reloadUI(stage, config);

            // Step 3: Restore the UI state
            restoreUIState(newController, uiState);

            return newController;
        } catch (Exception e) {
            logger.error("Error changing locale", e);
            throw new RuntimeException("Failed to change locale", e);
        }
    }

    private static UIState saveUIState(MainController controller) {
        UIState state = new UIState();

        // Save the state of ComboBoxes
        state.selectedProjectIndex = controller.getProjectComboBox().getSelectionModel().getSelectedIndex();
        state.selectedDestBranchIndex = controller.getDestBranchComboBox().getSelectionModel().getSelectedIndex();
        state.projectItems = controller.getProjectComboBox().getItems();
        state.destBranchesItems = controller.getDestBranchComboBox().getItems();

        // Save the state of TableView
        state.branchModels = controller.getBranchesTableView().getItems();
        state.selectedBranchIndices = controller.getBranchesTableView().getSelectionModel().getSelectedIndices();

        // Save the first selected item for scroll position restoration
        if (!state.selectedBranchIndices.isEmpty()) {
            int firstSelectedIndex = state.selectedBranchIndices.get(0);
            if (firstSelectedIndex >= 0 && firstSelectedIndex < state.branchModels.size()) {
                state.scrollToItem = state.branchModels.get(firstSelectedIndex);
            }
        }

        // Save other relevant state
        state.currentProjectId = controller.getCurrentProjectId();

        return state;
    }

    private static void restoreUIState(MainController controller, UIState state) {

        controller.getProjectComboBox().setItems(state.projectItems);
        controller.getDestBranchComboBox().setItems(state.destBranchesItems);
        state.projectItems.set(0, I18nUtil.getMessage("app.not.selected"));
        state.destBranchesItems.set(0, I18nUtil.getMessage("app.not.selected"));
        // Restore project selection
        if (state.selectedProjectIndex >= 0 && state.selectedProjectIndex < controller.getProjectComboBox().getItems().size()) {
            controller.getProjectComboBox().getSelectionModel().select(state.selectedProjectIndex);

            // This will trigger onProjectSelected() which will load branches
            // We need to wait for this to complete before continuing

            // Set the current project ID
            controller.setCurrentProjectId(state.currentProjectId);

            // Restore destination branch selection
            if (state.selectedDestBranchIndex >= 0 && 
                state.selectedDestBranchIndex < controller.getDestBranchComboBox().getItems().size()) {
                controller.getDestBranchComboBox().getSelectionModel().select(state.selectedDestBranchIndex);
                // This will trigger onMainBranchSelected()
            }
        }

        // Restore TableView state
        // This needs to be done after the branches are loaded
        controller.setBranchModels(state.branchModels);

        // Restore selection
        for (Integer index : state.selectedBranchIndices) {
            controller.getBranchesTableView().getSelectionModel().select(index);
        }

        // Restore scroll position
        if (state.scrollToItem != null) {
            controller.getBranchesTableView().scrollTo(state.scrollToItem);
        }

        // Update branch counter
        controller.refreshBranchCounter();
    }

    private static MainController reloadUI(Stage stage, AppConfig config) throws IOException {
        // Load the FXML with the new locale
        FXMLLoader fxmlLoader = new FXMLLoader(GitlaberApp.class.getResource("/fxml/main.fxml"));
        fxmlLoader.setResources(ResourceBundle.getBundle("i18n.messages", I18nUtil.getCurrentLocale()));

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

        // Update the stage title
        stage.setTitle(I18nUtil.getMessage("app.title"));

        // Set the new scene
        stage.setScene(scene);

        // Get the new controller
        MainController controller = fxmlLoader.getController();
        controller.initialize(config, stage);

        return controller;
    }


}
