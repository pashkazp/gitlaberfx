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
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.depavlo.gitlaberfx.util.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML
    private TextField gitlabUrlField;

    @FXML
    private PasswordField apiKeyField;

    @FXML
    private TextField usernameField;

    @FXML
    private ComboBox<String> languageComboBox;

    private AppConfig config;
    private Stage stage;
    private boolean saved = false;
    private Map<String, String> availableLocales = new HashMap<>();
    private MainController mainController;

    /**
     * Sets the main controller reference.
     * 
     * @param mainController The main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void initialize(AppConfig config, Stage stage) {
        this.config = config;
        this.stage = stage;

        gitlabUrlField.setText(config.getGitlabUrl());
        apiKeyField.setText(config.getApiKey());
        usernameField.setText(config.getUsername());

        // Initialize available locales
        initializeLocales();

        // Set the current locale in the ComboBox
        String currentLocale = config.getLocale();
        if (currentLocale != null && availableLocales.containsValue(currentLocale)) {
            // Find the display name for the current locale
            for (Map.Entry<String, String> entry : availableLocales.entrySet()) {
                if (entry.getValue().equals(currentLocale)) {
                    languageComboBox.getSelectionModel().select(entry.getKey());
                    break;
                }
            }
        } else {
            // Default to the first available locale if the current locale is not available
            if (!availableLocales.isEmpty()) {
                languageComboBox.getSelectionModel().select(availableLocales.keySet().iterator().next());
            } else {
                // If no locales are available, try to add a default one
                try {
                    // Try to get the language name from the default resource bundle
                    ResourceBundle defaultBundle = ResourceBundle.getBundle(I18nUtil.getBundleBaseName());
                    if (defaultBundle.containsKey("app.language.name")) {
                        String displayName = defaultBundle.getString("app.language.name");
                        availableLocales.put(displayName, "en_US"); // Assume default is English
                        languageComboBox.getItems().add(displayName);
                        languageComboBox.getSelectionModel().select(displayName);
                    } else {
                        // Fallback to hardcoded English
                        String displayName = "English";
                        availableLocales.put(displayName, "en_US");
                        languageComboBox.getItems().add(displayName);
                        languageComboBox.getSelectionModel().select(displayName);
                    }
                } catch (Exception e) {
                    logger.error("Error loading default resource bundle", e);
                    // Last resort fallback
                    String displayName = "English";
                    availableLocales.put(displayName, "en_US");
                    languageComboBox.getItems().add(displayName);
                    languageComboBox.getSelectionModel().select(displayName);
                }
            }
        }
    }

    /**
     * Initializes the available locales and populates the language ComboBox.
     * Dynamically searches for message files in the i18n directory and extracts locale information.
     * Works with both file system resources and JAR resources.
     */
    private void initializeLocales() {
        try {
            // Clear any existing locales
            availableLocales.clear();

            // Get the URL to the i18n directory
            URL i18nDirUrl = getClass().getClassLoader().getResource("i18n");
            if (i18nDirUrl == null) {
                logger.error("Could not find i18n directory");
                return;
            }

            List<String> messageFiles = new ArrayList<>();

            // Handle different resource types (file system or JAR)
            if (i18nDirUrl.getProtocol().equals("file")) {
                // File system resources
                Path i18nDirPath = Paths.get(i18nDirUrl.toURI());
                try (Stream<Path> paths = Files.list(i18nDirPath)) {
                    messageFiles = paths
                        .map(path -> path.getFileName().toString())
                        .filter(filename -> filename.matches("messages_[a-z]{2}_[A-Z]{2}\\.properties"))
                        .collect(Collectors.toList());
                }
            } else if (i18nDirUrl.getProtocol().equals("jar")) {
                // JAR resources
                String jarPath = i18nDirUrl.getPath().substring(5, i18nDirUrl.getPath().indexOf("!"));
                try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
                    messageFiles = jar.stream()
                        .map(JarEntry::getName)
                        .filter(name -> name.startsWith("i18n/") && name.matches("i18n/messages_[a-z]{2}_[A-Z]{2}\\.properties"))
                        .map(name -> name.substring(5)) // Remove "i18n/" prefix
                        .collect(Collectors.toList());
                }
            } else {
                // Try to use the resource bundle mechanism to find available locales
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

            // Process the found message files
            for (String filename : messageFiles) {
                if (filename.matches("messages_[a-z]{2}_[A-Z]{2}\\.properties")) {
                    // Extract locale code (e.g., "en_US" from "messages_en_US.properties")
                    String localeCode = filename.substring(9, filename.length() - 11); // 9 = "messages_".length(), 11 = ".properties".length()

                    // Create a Locale object to get the display name
                    String[] localeParts = localeCode.split("_");
                    if (localeParts.length == 2) {
                        Locale locale = new Locale(localeParts[0], localeParts[1]);
                        String displayName = locale.getDisplayName(I18nUtil.getCurrentLocale());

                        // Try to load the language name from the resource bundle for this locale
                        try {
                            // Create a temporary ResourceBundle for this locale to get its native language name
                            ResourceBundle tempBundle = ResourceBundle.getBundle(I18nUtil.getBundleBaseName(), locale);
                            if (tempBundle.containsKey("app.language.name")) {
                                displayName = tempBundle.getString("app.language.name");
                            }
                        } catch (Exception e) {
                            logger.debug("Could not load language name for locale {}, using display name: {}", localeCode, displayName);
                            // Fallback to the old method if there's an error
                            String key = "settings.language." + localeParts[0].toLowerCase();
                            String localizedName = I18nUtil.getMessage(key);
                            if (!localizedName.equals(key)) {
                                displayName = localizedName;
                            }
                        }

                        // Add to available locales
                        availableLocales.put(displayName, localeCode);
                        logger.debug("Found locale: {} ({})", displayName, localeCode);
                    }
                }
            }

            // If no locales were found, add default English locale
            if (availableLocales.isEmpty()) {
                logger.warn("No locale files found, adding default English locale");
                // Try to get the language name from the resource bundle
                String displayName = "English"; // Default fallback
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

            // Populate the ComboBox with locale display names
            languageComboBox.getItems().clear();
            languageComboBox.getItems().addAll(availableLocales.keySet());
        } catch (Exception e) {
            logger.error("Error initializing locales", e);
            // Fallback to hardcoded locales in case of error
            availableLocales.clear();

            // Try to get language names from resource bundles
            String englishName = "English"; // Default fallback
            String ukrainianName = "Українська"; // Default fallback

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

    @FXML
    private void testConnection() {
        try {
            AppConfig testConfig = new AppConfig();
            testConfig.setGitlabUrl(gitlabUrlField.getText());
            testConfig.setApiKey(apiKeyField.getText());
            testConfig.setUsername(usernameField.getText());

            GitLabService service = new GitLabService(testConfig);
            service.connect();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18nUtil.getMessage("app.success"));
            alert.setHeaderText(null);
            alert.setContentText(I18nUtil.getMessage("settings.connection.success"));
            alert.showAndWait();
        } catch (IOException e) {
            logger.error("Connection test failed", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18nUtil.getMessage("app.error"));
            alert.setHeaderText(null);
            alert.setContentText(I18nUtil.getMessage("settings.connection.error", e.getMessage()));
            alert.showAndWait();
        }
    }

    @FXML
    private void save() {
        config.setGitlabUrl(gitlabUrlField.getText());
        config.setApiKey(apiKeyField.getText());
        config.setUsername(usernameField.getText());

        // Save the selected locale
        String selectedLanguage = languageComboBox.getSelectionModel().getSelectedItem();
        boolean localeChanged = false;

        if (selectedLanguage != null && availableLocales.containsKey(selectedLanguage)) {
            String localeCode = availableLocales.get(selectedLanguage);
            String currentLocale = config.getLocale();

            // Check if locale has changed
            localeChanged = currentLocale != null && !currentLocale.equals(localeCode);

            if (localeChanged) {
                // Parse the locale code
                String[] localeParts = localeCode.split("_");
                if (localeParts.length == 2) {
                    Locale newLocale = new Locale(localeParts[0], localeParts[1]);

                    // Save other settings first
                    config.setLocale(localeCode);
                    config.save();
                    saved = true;

                    // Close the settings dialog
                    stage.close();

                    // Change locale dynamically
                    if (mainController != null) {
                        mainController.changeLocale(newLocale);
                    } else {
                        logger.error("MainController reference is null, cannot change locale dynamically");
                        // Fallback to old behavior
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

        // If locale didn't change or couldn't be parsed, just save and close
        config.save();
        saved = true;
        stage.close();
    }

    @FXML
    private void cancel() {
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }
} 
