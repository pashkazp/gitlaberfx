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
import com.depavlo.gitlaberfx.model.BranchModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service class for interacting with the GitLab API.
 * This class provides methods to perform various operations on GitLab resources
 * such as projects, branches, and commits. It handles the HTTP communication
 * with the GitLab server using OkHttp and parses JSON responses using Jackson.
 * 
 * The service supports operations like:
 * - Testing connection to GitLab
 * - Retrieving projects
 * - Managing branches (listing, checking merge status, deleting)
 * - Working with commits
 * 
 * It uses the configuration from AppConfig for GitLab URL and API token.
 */
public class GitLabService {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(GitLabService.class);
    /** Constant for the GitLab API private token header name. */
    public static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";

    /** Configuration containing GitLab URL and API token. */
    private final AppConfig config;

    /** HTTP client for making requests to the GitLab API. */
    private final OkHttpClient httpClient;

    /** Object mapper for parsing JSON responses from the GitLab API. */
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new GitLabService with the provided configuration.
     * Initializes the HTTP client with a custom SSL context that trusts all certificates,
     * which is useful for self-hosted GitLab instances with self-signed certificates.
     *
     * @param config the configuration containing GitLab URL and API token
     */
    public GitLabService(AppConfig config) {
        this.config = config;

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Failed to initialize custom SSL context", e);
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (sslContext != null) {
            builder.hostnameVerifier((hostname, session) -> true);
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
        }

        this.httpClient = builder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Performs a lightweight test connection to GitLab by fetching user details.
     * This validates both the URL and the API token.
     *
     * @throws IOException if the connection fails, the URL is invalid, or the token is incorrect.
     */
    public void testConnection() throws IOException {
        if (!config.isConfigurationValid()) {
            throw new IOException("GitLab URL or API Key is not configured correctly.");
        }
        logger.info("Testing connection to GitLab at {}", config.getGitlabUrl());
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(config.getGitlabUrl())).newBuilder()
                .addPathSegments("api/v4/user")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(PRIVATE_TOKEN, config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to connect to GitLab. Status: " + response.code() + " " + response.message());
            }
            logger.info("Connection successful.");
        }
    }


    /**
     * Retrieves a list of all GitLab projects that the user has access to.
     * This method handles pagination to fetch all projects, not just the first page.
     * It fetches projects in batches of 100 and continues until all projects are retrieved.
     *
     * @return a list of GitLab projects
     * @throws IOException if there is an error communicating with the GitLab API or parsing the response
     */
    public List<Project> getProjects() throws IOException {
        if (!config.isConfigurationValid()) {
            logger.warn("Skipping getProjects() because configuration is invalid.");
            return Collections.emptyList();
        }
        logger.debug("Getting projects list");
        List<Project> projects = new ArrayList<>();
        int page = 1;
        int perPage = 100;
        HttpUrl baseUrl = Objects.requireNonNull(HttpUrl.parse(config.getGitlabUrl()));

        while (true) {
            HttpUrl url = baseUrl.newBuilder()
                    .addPathSegments("api/v4/projects")
                    .addQueryParameter("per_page", String.valueOf(perPage))
                    .addQueryParameter("page", String.valueOf(page))
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .header(PRIVATE_TOKEN, config.getApiKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get projects: " + response);
                }
                JsonNode jsonArray = objectMapper.readTree(response.body().string());
                if (!jsonArray.isArray() || jsonArray.size() == 0) break;
                for (JsonNode projectNode : jsonArray) {
                    Project project = new Project();
                    project.setId(projectNode.get("id").asInt());
                    project.setName(projectNode.get("name").asText());
                    project.setPath(projectNode.get("path").asText());

                    if (projectNode.has("namespace") && projectNode.get("namespace").has("full_path")) {
                        project.setNamespacePath(projectNode.get("namespace").get("full_path").asText());
                    } else if (projectNode.has("path_with_namespace")) {
                        project.setNamespacePath(projectNode.get("path_with_namespace").asText());
                    }

                    projects.add(project);
                }
                if (jsonArray.size() < perPage) break;
                page++;
            }
        }
        return projects;
    }

    /**
     * Retrieves a list of all branches for a specific GitLab project.
     * This method handles pagination to fetch all branches, not just the first page.
     * It fetches branch details including name, last commit, merge status,
     * protection status, and other branch attributes.
     *
     * @param projectId the ID of the GitLab project
     * @return a list of branch models representing the branches in the project
     * @throws IOException if there is an error communicating with the GitLab API or parsing the response
     */
    public List<BranchModel> getBranches(String projectId) throws IOException {
        if (!config.isConfigurationValid()) {
            logger.warn("Skipping getBranches() because configuration is invalid.");
            return Collections.emptyList();
        }
        logger.debug("Getting branches for project {}", projectId);
        List<BranchModel> branches = new ArrayList<>();
        int page = 1;
        int perPage = 100;
        HttpUrl baseUrl = Objects.requireNonNull(HttpUrl.parse(config.getGitlabUrl()));

        while (true) {
            HttpUrl url = baseUrl.newBuilder()
                    .addPathSegments("api/v4/projects")
                    .addPathSegment(projectId)
                    .addPathSegments("repository/branches")
                    .addQueryParameter("per_page", String.valueOf(perPage))
                    .addQueryParameter("page", String.valueOf(page))
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .header(PRIVATE_TOKEN, config.getApiKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get branches: " + response);
                }
                JsonNode jsonArray = objectMapper.readTree(response.body().string());
                if (!jsonArray.isArray() || jsonArray.size() == 0) break;
                for (JsonNode branchNode : jsonArray) {
                    String branchName = branchNode.get("name").asText();
                    String lastCommitDate = branchNode.get("commit").get("committed_date").asText();
                    String lastCommitSha = branchNode.get("commit").get("id").asText();
                    boolean isProtected = branchNode.has("protected") && branchNode.get("protected").asBoolean();
                    boolean developersCanPush = branchNode.has("developers_can_push") && branchNode.get("developers_can_push").asBoolean();
                    boolean developersCanMerge = branchNode.has("developers_can_merge") && branchNode.get("developers_can_merge").asBoolean();
                    boolean canPush = branchNode.has("can_push") && branchNode.get("can_push").asBoolean();
                    boolean isDefault = branchNode.has("default") && branchNode.get("default").asBoolean();
                    boolean isMerged = branchNode.has("merged") && branchNode.get("merged").asBoolean();
                    branches.add(new BranchModel(branchName, lastCommitDate, lastCommitSha, isMerged, isProtected,
                                               developersCanPush, developersCanMerge, canPush, isDefault));
                }
                if (jsonArray.size() < perPage) break;
                page++;
            }
        }
        return branches;
    }

    /**
     * Archives a branch in a GitLab project by creating a new branch with the archive prefix
     * and then deleting the original branch. This operation is atomic - if any step fails,
     * the entire operation is rolled back.
     *
     * @param projectId the ID of the GitLab project
     * @param sourceBranchName the name of the branch to archive
     * @param archivePrefix the prefix to use for the archived branch
     * @param lastCommitSha SHA of the last commit in the branch
     * @throws IOException if there is an error communicating with the GitLab API or the branch cannot be archived
     */
    public void archiveBranch(String projectId, String sourceBranchName, String archivePrefix, String lastCommitSha) throws IOException {
        logger.info("Archiving branch {} (SHA: {}) from project {} with prefix {}", 
                    sourceBranchName, lastCommitSha, projectId, archivePrefix);

        // Form the new branch name
        String newBranchName = archivePrefix + sourceBranchName;

        // Step 1: Create the new (archive) branch
        HttpUrl createUrl = Objects.requireNonNull(HttpUrl.parse(config.getGitlabUrl())).newBuilder()
                .addPathSegments("api/v4/projects")
                .addPathSegment(projectId)
                .addPathSegments("repository/branches")
                .addQueryParameter("branch", newBranchName)
                .addQueryParameter("ref", sourceBranchName)
                .build();

        Request createRequest = new Request.Builder()
                .url(createUrl)
                .post(RequestBody.create(new byte[0], null))
                .header(PRIVATE_TOKEN, config.getApiKey())
                .build();

        try (Response createResponse = httpClient.newCall(createRequest).execute()) {
            if (!createResponse.isSuccessful()) {
                throw new IOException("Failed to create archive branch: " + createResponse);
            }
            logger.info("Successfully created archive branch {} from source branch {} (SHA: {})", 
                       newBranchName, sourceBranchName, lastCommitSha);

            // Step 2: Delete the original branch
            try {
                deleteBranch(projectId, sourceBranchName, lastCommitSha);
                logger.info("Successfully archived branch {} (SHA: {}) to {}", 
                           sourceBranchName, lastCommitSha, newBranchName);
            } catch (IOException deleteException) {
                // If deletion fails, roll back by deleting the newly created archive branch
                logger.error("Failed to delete source branch '{}' (SHA: {}). Attempting to roll back archive branch creation.", 
                            sourceBranchName, lastCommitSha, deleteException);
                try {
                    // Attempt rollback
                    deleteBranch(projectId, newBranchName, "unknown");
                    logger.info("Rollback successful: deleted archive branch '{}'", newBranchName);
                } catch (IOException rollbackException) {
                    // Log that rollback also failed, this is a critical situation
                    logger.error("ROLLBACK FAILED! Could not delete archive branch '{}'. Manual cleanup required.", 
                                newBranchName, rollbackException);
                }
                // Always re-throw the ORIGINAL exception to inform the user about the failure
                throw deleteException;
            }
        }
    }

    /**
     * Archives a branch in a GitLab project by creating a new branch with the archive prefix
     * and then deleting the original branch. This operation is atomic - if any step fails,
     * the entire operation is rolled back.
     * Overloaded method for backward compatibility.
     *
     * @param projectId the ID of the GitLab project
     * @param sourceBranchName the name of the branch to archive
     * @param archivePrefix the prefix to use for the archived branch
     * @throws IOException if there is an error communicating with the GitLab API or the branch cannot be archived
     */
    public void archiveBranch(String projectId, String sourceBranchName, String archivePrefix) throws IOException {
        archiveBranch(projectId, sourceBranchName, archivePrefix, "unknown");
    }

    /**
     * Deletes a branch from a GitLab project.
     *
     * @param projectId the ID of the GitLab project
     * @param branchName the name of the branch to delete
     * @param lastCommitSha SHA of the last commit in the branch
     * @throws IOException if there is an error communicating with the GitLab API or the branch cannot be deleted
     */
    public void deleteBranch(String projectId, String branchName, String lastCommitSha) throws IOException {
        logger.info("Deleting branch {} (SHA: {}) from project {}", branchName, lastCommitSha, projectId);
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(config.getGitlabUrl())).newBuilder()
                .addPathSegments("api/v4/projects")
                .addPathSegment(projectId)
                .addPathSegments("repository/branches")
                .addEncodedPathSegment(encodePathSegment(branchName))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .header(PRIVATE_TOKEN, config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to delete branch: " + response);
            }
            logger.info("Successfully deleted branch {} (SHA: {}) from project {}", branchName, lastCommitSha, projectId);
        }
    }

    /**
     * Deletes a branch from a GitLab project.
     * Overloaded method for backward compatibility.
     *
     * @param projectId the ID of the GitLab project
     * @param branchName the name of the branch to delete
     * @throws IOException if there is an error communicating with the GitLab API or the branch cannot be deleted
     */
    public void deleteBranch(String projectId, String branchName) throws IOException {
        deleteBranch(projectId, branchName, "unknown");
    }


    /**
     * Checks if a commit is present in a branch.
     * This method determines if the specified commit is part of the target branch's history.
     *
     * @param projectId the ID of the GitLab project
     * @param commitSha the SHA of the commit to check
     * @param targetBranchName the name of the target branch to check against
     * @return true if the commit is present in the target branch, false otherwise
     * @throws IOException if there is an error communicating with the GitLab API
     */
    public boolean isCommitInBranch(String projectId, String commitSha, String targetBranchName) throws IOException {
        logger.debug("Checking if commit {} is in branch {}", commitSha, targetBranchName);

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(config.getGitlabUrl())).newBuilder()
                .addPathSegments("api/v4/projects")
                .addPathSegment(projectId)
                .addPathSegments("repository/commits")
                .addPathSegment(commitSha)
                .addPathSegments("refs")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header(PRIVATE_TOKEN, config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to check if commit is in branch: {}", response);
                return false;
            }

            JsonNode refsArray = objectMapper.readTree(response.body().string());
            if (!refsArray.isArray()) {
                return false;
            }

            for (JsonNode refNode : refsArray) {
                if (refNode.has("type") && refNode.get("type").asText().equals("branch") &&
                    refNode.has("name") && refNode.get("name").asText().equals(targetBranchName)) {
                    return true;
                }
            }

            return false;
        } catch (IOException e) {
            logger.error("Error checking if commit is in branch", e);
            return false;
        }
    }

    /**
     * Represents a GitLab project with its basic properties.
     * This class is used to store project information retrieved from the GitLab API.
     */
    public static class Project {
        /** The unique identifier of the project in GitLab. */
        private int id;

        /** The name of the project. */
        private String name;

        /** The path segment of the project URL. */
        private String path;

        /** The namespace path of the project (e.g., group or user path). */
        private String namespacePath;

        /**
         * Gets the ID of the project.
         * 
         * @return the project ID
         */
        public int getId() { return id; }

        /**
         * Sets the ID of the project.
         * 
         * @param id the project ID to set
         */
        public void setId(int id) { this.id = id; }

        /**
         * Gets the name of the project.
         * 
         * @return the project name
         */
        public String getName() { return name; }

        /**
         * Sets the name of the project.
         * 
         * @param name the project name to set
         */
        public void setName(String name) { this.name = name; }

        /**
         * Gets the path segment of the project URL.
         * 
         * @return the project path
         */
        public String getPath() { return path; }

        /**
         * Sets the path segment of the project URL.
         * 
         * @param path the project path to set
         */
        public void setPath(String path) { this.path = path; }

        /**
         * Gets the namespace path of the project.
         * 
         * @return the namespace path
         */
        public String getNamespacePath() { return namespacePath; }

        /**
         * Sets the namespace path of the project.
         * 
         * @param namespacePath the namespace path to set
         */
        public void setNamespacePath(String namespacePath) { this.namespacePath = namespacePath; }

        /**
         * Gets the full path name of the project, including namespace.
         * If namespace path is available, returns "namespacePath/path", otherwise returns just the name.
         * 
         * @return the full path name of the project
         */
        public String getPathName() {
            if (namespacePath != null && !namespacePath.isEmpty()) {
                return namespacePath + "/" + path;
            }
            return name;
        }
    }

    /**
     * Encodes a branch name for use in a URL.
     * This method handles special characters in branch names to ensure they are properly encoded for GitLab API URLs.
     * It specifically pre-processes commas before applying standard URL encoding.
     *
     * @param segment the branch name to encode
     * @return the URL-encoded branch name, or null if the input was null
     */
    private String encodePathSegment(String segment) {
        if (segment == null) {
            return null;
        }
        return URLEncoder.encode(segment, StandardCharsets.UTF_8);
    }
}
