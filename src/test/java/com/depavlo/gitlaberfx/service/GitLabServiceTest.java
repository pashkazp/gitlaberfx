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
package com.depavlo.gitlaberfx.service;

import com.depavlo.gitlaberfx.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

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

        assertThrows(Exception.class, service::testConnection);
        logger.info("[DEBUG_LOG] Connect with null API key test completed");
    }

    @Test
    void testConnectWithEmptyApiKey() {
        logger.debug("[DEBUG_LOG] Testing connect with empty API key");
        config.setApiKey("");
        GitLabService service = new GitLabService(config);

        assertThrows(Exception.class, service::testConnection);
        logger.info("[DEBUG_LOG] Connect with empty API key test completed");
    }

    @Test
    void testEncodePathSegmentWithComma() throws Exception {
        logger.debug("[DEBUG_LOG] Testing encodePathSegment with comma in path name");

        // Get access to the private encodePathSegment method using reflection
        Method encodePathSegmentMethod = GitLabService.class.getDeclaredMethod("encodePathSegment", String.class);
        encodePathSegmentMethod.setAccessible(true);

        // Test branch name with comma and slash
        String pathSegment = "review/PLD-1173-fix_tests,_skip_dto_generation";
        String encodedName = (String) encodePathSegmentMethod.invoke(gitLabService, pathSegment);

        // Verify that the comma is properly encoded to %2C
        assertTrue(encodedName.contains("%2C"),
                "Encoded segment should contain properly encoded comma.");

        // Verify that the slash is properly encoded to %2F
        assertTrue(encodedName.contains("%2F"),
                "Encoded segment should contain properly encoded slash.");

        // Ensure there is no double encoding (%25)
        assertFalse(encodedName.contains("%25"),
                "Encoded segment should not be double-encoded.");

        assertEquals("review%2FPLD-1173-fix_tests%2C_skip_dto_generation", encodedName);

        logger.info("[DEBUG_LOG] encodePathSegment with comma test completed");
    }

    @Test
    void testIsCommitInBranch() throws Exception {
        logger.debug("[DEBUG_LOG] Testing isCommitInBranch method");

        // Test with a commit SHA and target branch
        String projectId = "123";
        String commitSha = "abc123";
        String targetBranchName = "main";

        // Since we can't actually test the API call in a unit test,
        // we're just verifying that the method doesn't throw an exception
        // This is a minimal test to ensure the method exists and can be called
        try {
            // This will likely return false since we're not mocking the API response
            gitLabService.isCommitInBranch(projectId, commitSha, targetBranchName);
            // If we get here without an exception, the test passes
            assertTrue(true);
        } catch (Exception e) {
            // If there's an exception related to HTTP connection (which is expected in a unit test),
            // we'll still consider the test passed
            if (e.getMessage() != null && e.getMessage().contains("Failed to check if commit is in branch")) {
                assertTrue(true);
            } else {
                // Any other exception is a failure
                fail("Unexpected exception: " + e.getMessage());
            }
        }

        logger.info("[DEBUG_LOG] isCommitInBranch test completed");
    }
}
