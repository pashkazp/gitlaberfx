package com.depavlo.gitlaberfx.service;

import com.depavlo.gitlaberfx.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class GitLabServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(GitLabServiceTest.class);
    private GitLabService gitLabService;
    private AppConfig config;

    @BeforeEach
    void setUp() {
        config = new AppConfig();
        config.setGitlabUrl("https://gitlab.com");
        config.setApiKey("test-api-key");
        gitLabService = new GitLabService(config);
    }

    @Test
    void testConfigInitialization() {
        logger.debug("[DEBUG_LOG] Testing configuration initialization");
        // Direct console output to verify line numbers in logs
        System.out.println("[DEBUG_LOG] This is a direct console output to verify line numbers in logs");
        assertNotNull(gitLabService);
        assertEquals("https://gitlab.com", config.getGitlabUrl());
        assertEquals("test-api-key", config.getApiKey());
        logger.info("[DEBUG_LOG] Configuration initialization test completed");
    }

    @Test
    void testConnectWithoutApiKey() {
        logger.debug("[DEBUG_LOG] Testing connect with null API key");
        config.setApiKey(null);
        GitLabService service = new GitLabService(config);

        assertThrows(Exception.class, () -> service.connect());
        logger.info("[DEBUG_LOG] Connect with null API key test completed");
    }

    @Test
    void testConnectWithEmptyApiKey() {
        logger.debug("[DEBUG_LOG] Testing connect with empty API key");
        config.setApiKey("");
        GitLabService service = new GitLabService(config);

        assertThrows(Exception.class, () -> service.connect());
        logger.info("[DEBUG_LOG] Connect with empty API key test completed");
    }
} 
