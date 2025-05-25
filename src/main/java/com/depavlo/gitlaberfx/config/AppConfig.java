package com.depavlo.gitlaberfx.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static String CONFIG_DIR = System.getProperty("user.home") + "/.config/gitlaberfx";
    private static String CONFIG_FILE = CONFIG_DIR + "/config.json";
    private static String OLD_CONFIG_DIR = System.getProperty("user.home") + "/.gitlaberfx";
    private static String OLD_CONFIG_FILE = OLD_CONFIG_DIR + "/config.json";

    // For testing purposes only
    static void setConfigPaths(String configDir, String configFile, String oldConfigDir, String oldConfigFile) {
        CONFIG_DIR = configDir;
        CONFIG_FILE = configFile;
        OLD_CONFIG_DIR = oldConfigDir;
        OLD_CONFIG_FILE = oldConfigFile;
    }

    // For testing purposes only
    static void resetConfigPaths() {
        CONFIG_DIR = System.getProperty("user.home") + "/.config/gitlaberfx";
        CONFIG_FILE = CONFIG_DIR + "/config.json";
        OLD_CONFIG_DIR = System.getProperty("user.home") + "/.gitlaberfx";
        OLD_CONFIG_FILE = OLD_CONFIG_DIR + "/config.json";
    }
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private String gitlabUrl;
    private String apiKey;
    private String username;
    private String lastProject;
    private String mainBranch;
    private List<String> excludedBranches;

    public AppConfig() {
        this.excludedBranches = new ArrayList<>();
    }

    public static AppConfig load() {
        try {
            createConfigDirIfNotExists();
            File configFile = new File(CONFIG_FILE);

            // Check if config file exists in the new location
            if (configFile.exists()) {
                return objectMapper.readValue(configFile, AppConfig.class);
            }

            // Check if config file exists in the old location
            File oldConfigFile = new File(OLD_CONFIG_FILE);
            if (oldConfigFile.exists()) {
                logger.info("Found configuration in old location. Migrating to new location.");
                AppConfig config = objectMapper.readValue(oldConfigFile, AppConfig.class);
                // Save to new location
                objectMapper.writeValue(configFile, config);
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
            objectMapper.writeValue(new File(CONFIG_FILE), this);
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

    public String getLastProject() {
        return lastProject;
    }

    public void setLastProject(String lastProject) {
        this.lastProject = lastProject;
    }

    public String getMainBranch() {
        return mainBranch;
    }

    public void setMainBranch(String mainBranch) {
        this.mainBranch = mainBranch;
    }

    public List<String> getExcludedBranches() {
        return excludedBranches;
    }

    public void setExcludedBranches(List<String> excludedBranches) {
        this.excludedBranches = excludedBranches;
    }
} 
