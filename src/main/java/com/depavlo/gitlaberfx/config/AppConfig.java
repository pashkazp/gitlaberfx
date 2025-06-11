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
package com.depavlo.gitlaberfx.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration class for the GitlaberFX application.
 * This class manages loading, saving, and accessing application configuration settings
 * such as GitLab URL, API key, and locale preferences.
 * Configuration is persisted to a properties file in the user's home directory.
 */
public class AppConfig {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    /** Directory path where configuration files are stored. */
    private static String CONFIG_DIR = System.getProperty("user.home") + "/.config/gitlaberfx";

    /** Full path to the configuration properties file. */
    private static String CONFIG_FILE = CONFIG_DIR + "/config.properties";

    /** The URL of the GitLab instance to connect to. */
    private String gitlabUrl;

    /** The API key or personal access token for authenticating with GitLab. */
    private String apiKey;

    /** The locale code for the application's user interface language. */
    private String locale;

    /** The prefix for archived branches. */
    private String archivePrefix = "archive/";

    /**
     * Constructs a new empty AppConfig instance.
     * This constructor creates a configuration object with null values for all properties.
     * Use the load() method to populate the configuration from the saved properties file.
     */
    public AppConfig() {
    }

    /**
     * Checks if the essential configuration (URL and API Key) is present and appears valid.
     * @return true if configuration is valid, false otherwise.
     */
    public boolean isConfigurationValid() {
        if (gitlabUrl == null || gitlabUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            return false;
        }
        try {
            // A simple check to ensure the URL is well-formed.
            new URL(gitlabUrl);
            return true;
        } catch (MalformedURLException e) {
            logger.warn("Invalid GitLab URL format: {}", gitlabUrl);
            return false;
        }
    }

    /**
     * Loads the application configuration from the properties file.
     * If the configuration file exists, it reads the properties and creates a populated AppConfig object.
     * If the file doesn't exist or there's an error reading it, returns a new empty AppConfig object.
     *
     * @return a populated AppConfig object if the file exists and can be read, otherwise a new empty AppConfig
     */
    public static AppConfig load() {
        try {
            createConfigDirIfNotExists();
            File configFile = new File(CONFIG_FILE);
            AppConfig config = new AppConfig();

            if (configFile.exists()) {
                Properties properties = new Properties();
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                }

                config.setGitlabUrl(properties.getProperty("gitlabUrl"));
                config.setApiKey(properties.getProperty("apiKey"));
                config.setLocale(properties.getProperty("locale", "en_US"));
                config.setArchivePrefix(properties.getProperty("archivePrefix", "archive/"));

                return config;
            }

        } catch (IOException e) {
            logger.error("Error loading configuration", e);
        }
        return new AppConfig();
    }

    /**
     * Saves the current configuration to the properties file.
     * This method creates the configuration directory if it doesn't exist,
     * then writes all non-null properties to the file.
     * If there's an error during the save operation, it's logged but not propagated.
     */
    public void save() {
        try {
            createConfigDirIfNotExists();

            Properties properties = new Properties();

            if (gitlabUrl != null) properties.setProperty("gitlabUrl", gitlabUrl);
            if (apiKey != null) properties.setProperty("apiKey", apiKey);
            if (locale != null) properties.setProperty("locale", locale);
            if (archivePrefix != null) properties.setProperty("archivePrefix", archivePrefix);

            try (FileOutputStream fos = new FileOutputStream(new File(CONFIG_FILE))) {
                properties.store(fos, "GitLaberFX Configuration");
            }
        } catch (IOException e) {
            logger.error("Error saving configuration", e);
        }
    }

    /**
     * Creates the configuration directory if it doesn't exist.
     * This method is called before any file operations to ensure the directory structure is in place.
     *
     * @throws IOException if there's an error creating the directory
     */
    private static void createConfigDirIfNotExists() throws IOException {
        Path configPath = Paths.get(CONFIG_DIR);
        if (!Files.exists(configPath)) {
            Files.createDirectories(configPath);
        }
    }

    // Getters and setters
    /**
     * Gets the GitLab URL.
     *
     * @return the URL of the GitLab instance
     */
    public String getGitlabUrl() {
        return gitlabUrl;
    }

    /**
     * Sets the GitLab URL.
     *
     * @param gitlabUrl the URL of the GitLab instance to set
     */
    public void setGitlabUrl(String gitlabUrl) {
        this.gitlabUrl = gitlabUrl;
    }

    /**
     * Gets the API key for GitLab authentication.
     *
     * @return the API key or personal access token
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the API key for GitLab authentication.
     *
     * @param apiKey the API key or personal access token to set
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Gets the locale code for the application's user interface language.
     *
     * @return the locale code (e.g., "en_US", "uk_UA")
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Sets the locale code for the application's user interface language.
     *
     * @param locale the locale code to set (e.g., "en_US", "uk_UA")
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Gets the prefix for archived branches.
     *
     * @return the prefix for archived branches
     */
    public String getArchivePrefix() {
        return archivePrefix;
    }

    /**
     * Sets the prefix for archived branches.
     *
     * @param archivePrefix the prefix for archived branches to set
     */
    public void setArchivePrefix(String archivePrefix) {
        this.archivePrefix = archivePrefix;
    }
}
