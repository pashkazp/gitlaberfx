package com.depavlo.gitlaberfx.service;

import com.depavlo.gitlaberfx.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitLabServiceTest {

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
        assertNotNull(gitLabService);
        assertEquals("https://gitlab.com", config.getGitlabUrl());
        assertEquals("test-api-key", config.getApiKey());
    }

    @Test
    void testConnectWithoutApiKey() {
        config.setApiKey(null);
        GitLabService service = new GitLabService(config);
        
        assertThrows(Exception.class, () -> service.connect());
    }

    @Test
    void testConnectWithEmptyApiKey() {
        config.setApiKey("");
        GitLabService service = new GitLabService(config);
        
        assertThrows(Exception.class, () -> service.connect());
    }
} 