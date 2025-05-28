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
