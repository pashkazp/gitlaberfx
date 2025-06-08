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
import com.depavlo.gitlaberfx.util.I18nUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controller for the settings dialog.
 * This class handles the functionality of the settings dialog, which allows
 * the user to configure GitLab connection settings and application preferences.
 * It manages GitLab URL, API key, and language selection, and provides
 * functionality to test the connection and save the settings.
 */
public class SettingsController {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    /** Text field for entering the GitLab URL. */
    @FXML
    private TextField gitlabUrlField;

    /** Password field for entering the GitLab API key. */
    @FXML
    private PasswordField apiKeyField;

    /** Combo box for selecting the application language. */
    @FXML
    private ComboBox<String> languageComboBox;

    /** The application configuration that will be updated with the new settings. */
    private AppConfig config;

    /** The stage that contains the settings dialog. */
    private Stage stage;

    /** Flag indicating whether the settings have been saved. */
    private boolean saved = false;

    /** Map of available locales, with display names as keys and locale codes as values. */
    private Map<String, String> availableLocales = new HashMap<>();

    /** Reference to the main controller for updating the UI after settings changes. */
    private MainController mainController;

    /**
     * Sets the main controller reference.
     * This method is called by the main controller to establish a reference
     * that allows the settings controller to communicate with the main controller.
     *
     * @param mainController The main controller instance
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Initializes the controller with the application configuration and stage.
     * This method is called after the FXML has been loaded.
     * It populates the form fields with the current configuration values
     * and initializes the available locales for the language selection.
     *
     * @param config The application configuration containing the current settings
     * @param stage The stage that contains the settings dialog
     */
    public void initialize(AppConfig config, Stage stage) {
        this.config = config;
        this.stage = stage;

        gitlabUrlField.setText(config.getGitlabUrl());
        apiKeyField.setText(config.getApiKey());

        initializeLocales();

        String currentLocale = config.getLocale();
        if (currentLocale != null && availableLocales.containsValue(currentLocale)) {
            for (Map.Entry<String, String> entry : availableLocales.entrySet()) {
                if (entry.getValue().equals(currentLocale)) {
                    languageComboBox.getSelectionModel().select(entry.getKey());
                    break;
                }
            }
        } else {
            if (!availableLocales.isEmpty()) {
                languageComboBox.getSelectionModel().select(availableLocales.keySet().iterator().next());
            } else {
                try {
                    ResourceBundle defaultBundle = ResourceBundle.getBundle(I18nUtil.getBundleBaseName());
                    if (defaultBundle.containsKey("app.language.name")) {
                        String displayName = defaultBundle.getString("app.language.name");
                        availableLocales.put(displayName, "en_US");
                        languageComboBox.getItems().add(displayName);
                        languageComboBox.getSelectionModel().select(displayName);
                    } else {
                        String displayName = "English";
                        availableLocales.put(displayName, "en_US");
                        languageComboBox.getItems().add(displayName);
                        languageComboBox.getSelectionModel().select(displayName);
                    }
                } catch (Exception e) {
                    logger.error("Error loading default resource bundle", e);
                    String displayName = "English";
                    availableLocales.put(displayName, "en_US");
                    languageComboBox.getItems().add(displayName);
                    languageComboBox.getSelectionModel().select(displayName);
                }
            }
        }
    }

    /**
     * Initializes the available locales for the language selection.
     * This method scans the i18n directory for message property files
     * and extracts the available locales. It populates the language combo box
     * with the display names of the available locales.
     * The method handles both file system and JAR-based resource access.
     */
    private void initializeLocales() {
        try {
            availableLocales.clear();

            URL i18nDirUrl = getClass().getClassLoader().getResource("i18n");
            if (i18nDirUrl == null) {
                logger.error("Could not find i18n directory");
                return;
            }

            List<String> messageFiles = new ArrayList<>();

            if (i18nDirUrl.getProtocol().equals("file")) {
                Path i18nDirPath = Paths.get(i18nDirUrl.toURI());
                try (Stream<Path> paths = Files.list(i18nDirPath)) {
                    messageFiles = paths
                            .map(path -> path.getFileName().toString())
                            .filter(filename -> filename.matches("messages_[a-z]{2}_[A-Z]{2}\\.properties"))
                            .collect(Collectors.toList());
                }
            } else if (i18nDirUrl.getProtocol().equals("jar")) {
                String jarPath = i18nDirUrl.getPath().substring(5, i18nDirUrl.getPath().indexOf("!"));
                try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
                    messageFiles = jar.stream()
                            .map(JarEntry::getName)
                            .filter(name -> name.startsWith("i18n/") && name.matches("i18n/messages_[a-z]{2}_[A-Z]{2}\\.properties"))
                            .map(name -> name.substring(5))
                            .collect(Collectors.toList());
                }
            } else {
                ResourceBundle.Control control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES);
                List<Locale> availableLocalesList = control.getCandidateLocales("i18n.messages", Locale.getDefault());
                for (Locale locale : availableLocalesList) {
                    if (!locale.toString().isEmpty()) {
                        messageFiles.add("messages_" + locale + ".properties");
                    }
                }

                if (messageFiles.isEmpty()) {
                    logger.warn("Could not determine available locales from resource URL: {}", i18nDirUrl);
                }
            }

            for (String filename : messageFiles) {
                if (filename.matches("messages_[a-z]{2}_[A-Z]{2}\\.properties")) {
                    String localeCode = filename.substring(9, filename.length() - 11);

                    String[] localeParts = localeCode.split("_");
                    if (localeParts.length == 2) {
                        Locale locale = new Locale(localeParts[0], localeParts[1]);
                        String displayName = locale.getDisplayName(I18nUtil.getCurrentLocale());

                        try {
                            ResourceBundle tempBundle = ResourceBundle.getBundle(I18nUtil.getBundleBaseName(), locale);
                            if (tempBundle.containsKey("app.language.name")) {
                                displayName = tempBundle.getString("app.language.name");
                            }
                        } catch (Exception e) {
                            logger.debug("Could not load language name for locale {}, using display name: {}", localeCode, displayName);
                            String key = "settings.language." + localeParts[0].toLowerCase();
                            String localizedName = I18nUtil.getMessage(key);
                            if (!localizedName.equals(key)) {
                                displayName = localizedName;
                            }
                        }

                        availableLocales.put(displayName, localeCode);
                        logger.debug("Found locale: {} ({})", displayName, localeCode);
                    }
                }
            }

            if (availableLocales.isEmpty()) {
                logger.warn("No locale files found, adding default English locale");
                String displayName = "English";
                try {
                    ResourceBundle defaultBundle = ResourceBundle.getBundle(I18nUtil.getBundleBaseName(), new Locale("en", "US"));
                    if (defaultBundle.containsKey("app.language.name")) {
                        displayName = defaultBundle.getString("app.language.name");
                    } else {
                        displayName = I18nUtil.getMessage("settings.language.english");
                    }
                } catch (Exception e) {
                    logger.debug("Could not load language name for default locale, using fallback");
                    displayName = I18nUtil.getMessage("settings.language.english");
                }
                availableLocales.put(displayName, "en_US");
            }

            languageComboBox.getItems().clear();
            languageComboBox.getItems().addAll(availableLocales.keySet());
        } catch (Exception e) {
            logger.error("Error initializing locales", e);
            availableLocales.clear();

            String englishName = "English";
            String ukrainianName = I18nUtil.getMessage("settings.language.ukrainian");

            try {
                ResourceBundle enBundle = ResourceBundle.getBundle(I18nUtil.getBundleBaseName(), new Locale("en", "US"));
                if (enBundle.containsKey("app.language.name")) {
                    englishName = enBundle.getString("app.language.name");
                } else {
                    englishName = I18nUtil.getMessage("settings.language.english");
                }
            } catch (Exception ex) {
                logger.debug("Could not load English language name, using fallback");
                englishName = I18nUtil.getMessage("settings.language.english");
            }

            try {
                ResourceBundle ukBundle = ResourceBundle.getBundle(I18nUtil.getBundleBaseName(), new Locale("uk", "UA"));
                if (ukBundle.containsKey("app.language.name")) {
                    ukrainianName = ukBundle.getString("app.language.name");
                } else {
                    ukrainianName = I18nUtil.getMessage("settings.language.ukrainian");
                }
            } catch (Exception ex) {
                logger.debug("Could not load Ukrainian language name, using fallback");
                ukrainianName = I18nUtil.getMessage("settings.language.ukrainian");
            }

            availableLocales.put(englishName, "en_US");
            availableLocales.put(ukrainianName, "uk_UA");
            languageComboBox.getItems().clear();
            languageComboBox.getItems().addAll(availableLocales.keySet());
        }
    }

    /**
     * Tests the connection to the GitLab server using the current settings.
     * This method is called when the user clicks the test connection button.
     * It creates a temporary configuration with the current form values
     * and attempts to connect to the GitLab server. The result is displayed
     * in an alert dialog.
     */
    @FXML
    private void testConnection() {
        AppConfig testConfig = new AppConfig();
        testConfig.setGitlabUrl(gitlabUrlField.getText());
        testConfig.setApiKey(apiKeyField.getText());

        if (!testConfig.isConfigurationValid()) {
            showError(I18nUtil.getMessage("app.error"), I18nUtil.getMessage("warning.missing.settings.message"));
            return;
        }

        GitLabService service = new GitLabService(testConfig);
        try {
            service.testConnection();
            showSuccess(I18nUtil.getMessage("app.success"), I18nUtil.getMessage("settings.connection.success"));
        } catch (IOException e) {
            logger.error("Connection test failed", e);
            showError(I18nUtil.getMessage("app.error"), I18nUtil.getMessage("settings.connection.error.generic"));
        }
    }

    /**
     * Saves the settings and closes the dialog.
     * This method is called when the user clicks the save button.
     * It updates the application configuration with the values from the form fields
     * and saves the configuration to disk. If the language has been changed,
     * it also updates the application locale and notifies the main controller.
     */
    @FXML
    private void save() {
        config.setGitlabUrl(gitlabUrlField.getText());
        config.setApiKey(apiKeyField.getText());

        String selectedLanguage = languageComboBox.getSelectionModel().getSelectedItem();
        boolean localeChanged = false;

        if (selectedLanguage != null && availableLocales.containsKey(selectedLanguage)) {
            String localeCode = availableLocales.get(selectedLanguage);
            String currentLocale = config.getLocale();

            localeChanged = currentLocale != null && !currentLocale.equals(localeCode);

            if (localeChanged) {
                String[] localeParts = localeCode.split("_");
                if (localeParts.length == 2) {
                    Locale newLocale = new Locale(localeParts[0], localeParts[1]);

                    config.setLocale(localeCode);
                    config.save();
                    saved = true;

                    stage.close();

                    if (mainController != null) {
                        mainController.changeLocale(newLocale);
                    } else {
                        logger.error("MainController reference is null, cannot change locale dynamically");
                        I18nUtil.setLocale(newLocale);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(I18nUtil.getMessage("app.info"));
                        alert.setHeaderText(null);
                        alert.setContentText(I18nUtil.getMessage("settings.locale.restart"));
                        alert.showAndWait();
                    }

                    return;
                }
            }
        }

        config.save();
        saved = true;
        stage.close();
    }

    /**
     * Cancels the settings changes and closes the dialog.
     * This method is called when the user clicks the cancel button.
     * It discards any changes made to the form fields and closes the dialog
     * without updating the application configuration.
     */
    @FXML
    private void cancel() {
        stage.close();
    }

    /**
     * Checks if the settings have been saved.
     * This method can be used by the calling code to determine if the user
     * saved the settings or cancelled the dialog.
     *
     * @return true if the settings were saved, false otherwise
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * Displays a success message in an information dialog.
     * This method is used to show success messages to the user,
     * such as when a connection test is successful.
     *
     * @param title The title of the dialog
     * @param message The message to display in the dialog
     */
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays an error message in an error dialog.
     * This method is used to show error messages to the user,
     * such as when a connection test fails or when required settings are missing.
     *
     * @param title The title of the dialog
     * @param message The error message to display in the dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
