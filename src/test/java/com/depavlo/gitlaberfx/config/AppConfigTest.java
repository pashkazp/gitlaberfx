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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    private String originalConfigDir;
    private String originalConfigFile;
    private Path testConfigFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Store original values
        originalConfigDir = getStaticFieldValue("CONFIG_DIR");
        originalConfigFile = getStaticFieldValue("CONFIG_FILE");

        // Create test config file path in the temporary directory
        testConfigFile = tempDir.resolve("config.properties");

        // Set test values using reflection
        setStaticFieldValue("CONFIG_DIR", tempDir.toString());
        setStaticFieldValue("CONFIG_FILE", testConfigFile.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restore original values
        setStaticFieldValue("CONFIG_DIR", originalConfigDir);
        setStaticFieldValue("CONFIG_FILE", originalConfigFile);
        // Temporary directory and its contents will be cleaned up automatically by JUnit
    }

    private String getStaticFieldValue(String fieldName) throws Exception {
        Field field = AppConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private void setStaticFieldValue(String fieldName, String value) throws Exception {
        Field field = AppConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    @Test
    void testSaveAndLoad() throws IOException {
        // Create test config
        AppConfig config = new AppConfig();
        config.setGitlabUrl("https://gitlab.com");
        config.setApiKey("test-api-key");
        config.setLocale("uk_UA");

        // Save config
        config.save();

        // Verify file was created
        assertTrue(Files.exists(testConfigFile), "Config file should exist in temporary directory");

        // Load config
        AppConfig loadedConfig = AppConfig.load();

        // Verify values
        assertEquals("https://gitlab.com", loadedConfig.getGitlabUrl(), "GitLab URL should match");
        assertEquals("test-api-key", loadedConfig.getApiKey(), "API key should match");
        assertEquals("uk_UA", loadedConfig.getLocale(), "Locale should match");
    }

    @Test
    void testEmptyConfig() throws IOException {
        // Ensure config file does not exist for this specific test case
        if (Files.exists(testConfigFile)) {
            Files.delete(testConfigFile);
        }

        // Create empty config (it shouldn't try to load from a non-existent file on construction)
        AppConfig config = new AppConfig();

        // Verify empty values for a newly created config object
        assertNull(config.getGitlabUrl(), "GitLab URL should be null for a new config");
        assertNull(config.getApiKey(), "API key should be null for a new config");
        assertNull(config.getLocale(), "Locale should be null for a new config");

        // Optionally, test loading from a non-existent/empty file if AppConfig.load() handles this
        // AppConfig loadedConfig = AppConfig.load();
        // assertNull(loadedConfig.getGitlabUrl(), "GitLab URL should be null when loaded from empty/non-existent file");
        // assertNull(loadedConfig.getApiKey(), "API key should be null when loaded from empty/non-existent file");
        // assertNull(loadedConfig.getLocale(), "Locale should be null when loaded from empty/non-existent file");
    }
}