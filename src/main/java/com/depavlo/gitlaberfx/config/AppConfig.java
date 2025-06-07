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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static String CONFIG_DIR = System.getProperty("user.home") + "/.config/gitlaberfx";
    private static String CONFIG_FILE = CONFIG_DIR + "/config.properties";

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private String gitlabUrl;
    private String apiKey;
    private String username;
    private String locale;

    public AppConfig() {
    }

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
                config.setUsername(properties.getProperty("username"));
                config.setLocale(properties.getProperty("locale", "en_US"));

                return config;
            }

        } catch (IOException e) {
            logger.error("Error loading configuration", e);
        }
        return new AppConfig();
    }

    public void save() {
        try {
            createConfigDirIfNotExists();

            Properties properties = new Properties();

            if (gitlabUrl != null) properties.setProperty("gitlabUrl", gitlabUrl);
            if (apiKey != null) properties.setProperty("apiKey", apiKey);
            if (username != null) properties.setProperty("username", username);
            if (locale != null) properties.setProperty("locale", locale);

            try (FileOutputStream fos = new FileOutputStream(new File(CONFIG_FILE))) {
                properties.store(fos, "GitLaberFX Configuration");
            }
        } catch (IOException e) {
            logger.error("Error saving configuration", e);
        }
    }

    private static void createConfigDirIfNotExists() throws IOException {
        Path configPath = Paths.get(CONFIG_DIR);
        if (!Files.exists(configPath)) {
            Files.createDirectories(configPath);
        }
    }

    // Getters and setters
    public String getGitlabUrl() {
        return gitlabUrl;
    }

    public void setGitlabUrl(String gitlabUrl) {
        this.gitlabUrl = gitlabUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
