package com.depavlo.gitlaberfx.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoad() throws IOException {
        // Створюємо тестовий конфіг
        AppConfig config = new AppConfig();
        config.setGitlabUrl("https://gitlab.com");
        config.setApiKey("test-api-key");
        config.setUsername("test-user");
        config.setLastProject("test-project");
        config.setMainBranch("main");
        config.setExcludedBranches(Arrays.asList("branch1", "branch2"));

        // Зберігаємо конфіг
        File configFile = tempDir.resolve("config.json").toFile();
        config.save();

        // Завантажуємо конфіг
        AppConfig loadedConfig = AppConfig.load();

        // Перевіряємо значення
        assertEquals("https://gitlab.com", loadedConfig.getGitlabUrl());
        assertEquals("test-api-key", loadedConfig.getApiKey());
        assertEquals("test-user", loadedConfig.getUsername());
        assertEquals("test-project", loadedConfig.getLastProject());
        assertEquals("main", loadedConfig.getMainBranch());
        assertEquals(Arrays.asList("branch1", "branch2"), loadedConfig.getExcludedBranches());
    }

    @Test
    void testEmptyConfig() {
        AppConfig config = new AppConfig();
        assertNull(config.getGitlabUrl());
        assertNull(config.getApiKey());
        assertNull(config.getUsername());
        assertNull(config.getLastProject());
        assertNull(config.getMainBranch());
        assertTrue(config.getExcludedBranches().isEmpty());
    }
} 