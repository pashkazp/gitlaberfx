package com.depavlo.gitlaberfx.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Set test values
        String testConfigDir = tempDir.toString() + "/.config/gitlaberfx";
        String testConfigFile = testConfigDir + "/config.properties";
        String testOldConfigDir = tempDir.toString() + "/.gitlaberfx";
        String testOldConfigFile = testOldConfigDir + "/config.json";

        AppConfig.setConfigPaths(testConfigDir, testConfigFile, testOldConfigDir, testOldConfigFile);
    }

    @AfterEach
    void tearDown() {
        // Restore original values
        AppConfig.resetConfigPaths();
    }

    @Test
    void testSaveAndLoad() throws IOException {
        // Створюємо тестовий конфіг
        AppConfig config = new AppConfig();
        config.setGitlabUrl("https://gitlab.com");
        config.setApiKey("test-api-key");
        config.setUsername("test-user");
        config.setExcludedBranches(Arrays.asList("branch1", "branch2"));

        // Зберігаємо конфіг
        config.save();

        // Завантажуємо конфіг
        AppConfig loadedConfig = AppConfig.load();

        // Перевіряємо значення
        assertEquals("https://gitlab.com", loadedConfig.getGitlabUrl());
        assertEquals("test-api-key", loadedConfig.getApiKey());
        assertEquals("test-user", loadedConfig.getUsername());
        assertEquals(Arrays.asList("branch1", "branch2"), loadedConfig.getExcludedBranches());
    }

    @Test
    void testEmptyConfig() {
        AppConfig config = new AppConfig();
        assertNull(config.getGitlabUrl());
        assertNull(config.getApiKey());
        assertNull(config.getUsername());
        assertTrue(config.getExcludedBranches().isEmpty());
    }

    @Test
    void testMigrationFromOldLocation() throws Exception {
        // Create a config in the old location
        AppConfig oldConfig = new AppConfig();
        oldConfig.setGitlabUrl("https://old-gitlab.com");
        oldConfig.setApiKey("old-api-key");
        oldConfig.setUsername("old-user");

        // Ensure old config directory exists
        String oldConfigDir = tempDir.toString() + "/.gitlaberfx";
        new File(oldConfigDir).mkdirs();

        // Save to old location
        File oldConfigFile = new File(oldConfigDir + "/config.json");
        new ObjectMapper().writeValue(oldConfigFile, oldConfig);

        // Load config (should migrate from old to new location)
        AppConfig loadedConfig = AppConfig.load();

        // Verify migration
        assertEquals("https://old-gitlab.com", loadedConfig.getGitlabUrl());
        assertEquals("old-api-key", loadedConfig.getApiKey());
        assertEquals("old-user", loadedConfig.getUsername());

        // Verify config was saved to new location
        File newConfigFile = new File(tempDir.toString() + "/.config/gitlaberfx/config.properties");
        assertTrue(newConfigFile.exists());
    }

}
