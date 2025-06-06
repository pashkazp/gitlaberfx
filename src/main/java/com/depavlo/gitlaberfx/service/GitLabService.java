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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GitLabService {
    private static final Logger logger = LoggerFactory.getLogger(GitLabService.class);
    public static final String API_V_4_PROJECTS = "/api/v4/projects/";
    public static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";
    private final AppConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Checks if all required configuration fields for GitLab connection are present.
     * @return true if all required fields are present, false otherwise
     */
    public boolean hasRequiredConfig() {
        return config.getGitlabUrl() != null && !config.getGitlabUrl().isEmpty() &&
               config.getApiKey() != null && !config.getApiKey().isEmpty();
    }

    public GitLabService(AppConfig config) {
        this.config = config;

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{TRUST_ALL_CERTS}, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        this.httpClient = new OkHttpClient.Builder()
                .hostnameVerifier((hostname, session) -> true)
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) TRUST_ALL_CERTS)
                .build();


//        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }
    };

    public void connect() throws IOException {
        if (!hasRequiredConfig()) {
            throw new IOException("Required GitLab configuration is missing. Please check GitLab URL and API key in settings.");
        }
        logger.info("Connecting to GitLab at {}", config.getGitlabUrl());
    }

    public List<Project> getProjects() throws IOException {
        logger.debug("Getting projects list");
        List<Project> projects = new ArrayList<>();
        int page = 1;
        int perPage = 100;
        while (true) {
            String url = config.getGitlabUrl() + "/api/v4/projects?per_page=" + perPage + "&page=" + page;
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

                    // Extract namespace path from the response
                    if (projectNode.has("namespace") && projectNode.get("namespace").has("full_path")) {
                        project.setNamespacePath(projectNode.get("namespace").get("full_path").asText());
                    } else if (projectNode.has("path_with_namespace")) {
                        // Alternative: use path_with_namespace if available
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

    public List<BranchModel> getBranches(String projectId) throws IOException {
        logger.debug("Getting branches for project {}", projectId);
        List<BranchModel> branches = new ArrayList<>();
        int page = 1;
        int perPage = 100;
        while (true) {
            String url = config.getGitlabUrl() + API_V_4_PROJECTS + projectId + "/repository/branches?per_page=" + perPage + "&page=" + page;
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
                    // Extract additional boolean properties
                    boolean isProtected = branchNode.has("protected") ? branchNode.get("protected").asBoolean() : false;
                    boolean developersCanPush = branchNode.has("developers_can_push") ? branchNode.get("developers_can_push").asBoolean() : false;
                    boolean developersCanMerge = branchNode.has("developers_can_merge") ? branchNode.get("developers_can_merge").asBoolean() : false;
                    boolean canPush = branchNode.has("can_push") ? branchNode.get("can_push").asBoolean() : false;
                    boolean isDefault = branchNode.has("default") ? branchNode.get("default").asBoolean() : false;
                    boolean isMerged = branchNode.has("merged") ? branchNode.get("merged").asBoolean() : false;
                    // Initialize merged flag to false, it will be updated when a main branch is selected
                    branches.add(new BranchModel(branchName, lastCommitDate, isMerged, isProtected,
                                               developersCanPush, developersCanMerge, canPush, isDefault));
                }
                if (jsonArray.size() < perPage) break;
                page++;
            }
        }
        return branches;
    }

    public void deleteBranch(String projectId, String branchName) throws IOException {
        logger.info("Deleting branch {} from project {}", branchName, projectId);
        String url = config.getGitlabUrl() + API_V_4_PROJECTS + projectId + "/repository/branches/" + encodeBranchName(branchName);
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .header(PRIVATE_TOKEN, config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to delete branch: " + response);
            }
        }
    }


    public boolean isCommitInMainBranch(String projectId, String branchName, String mainBranch) throws IOException {
        logger.debug("Checking if branch {} is merged into {}", branchName, mainBranch);

        // Check if the source branch and target branch are the same
        if (branchName.equals(mainBranch)) {
            logger.debug("Source branch and target branch are the same, returning false");
            return false;
        }

        // Step 1: Get the SHA of the last commit of the source branch
        String sourceBranchSha = getBranchLastCommitSha(projectId, branchName);
        if (sourceBranchSha == null) {
            logger.error("Failed to get SHA for branch {}", branchName);
            return false;
        }

        // Step 2: Get the SHA of the merge base between source and target branches
        String mergeBaseSha = getMergeBaseSha(projectId, branchName, mainBranch);
        if (mergeBaseSha == null) {
            logger.error("Failed to get merge base SHA between {} and {}", branchName, mainBranch);
            return false;
        }

        // Step 3: Compare the SHAs - if they're identical, the source branch is merged into the target branch
        boolean isMerged = sourceBranchSha.equals(mergeBaseSha);
        logger.debug("Branch {} is {} into {}", branchName, isMerged ? "merged" : "not merged", mainBranch);
        return isMerged;
    }

    /**
     * Get the SHA of the last commit of a branch
     *
     * @param projectId the project ID
     * @param branchName the branch name
     * @return the SHA of the last commit of the branch, or null if an error occurred
     */
    private String getBranchLastCommitSha(String projectId, String branchName) {
        logger.debug("Getting SHA for branch {}", branchName);
        String url = config.getGitlabUrl() + API_V_4_PROJECTS + projectId + "/repository/branches/" + encodeBranchName(branchName);
        Request request = new Request.Builder()
                .url(url)
                .header(PRIVATE_TOKEN, config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to get branch {}: {}", branchName, response);
                return null;
            }
            JsonNode branchNode = objectMapper.readTree(response.body().string());
            return branchNode.get("commit").get("id").asText();
        } catch (IOException e) {
            logger.error("Error getting SHA for branch {}", branchName, e);
            return null;
        }
    }

    /**
     * Get the SHA of the merge base between two branches
     *
     * @param projectId the project ID
     * @param sourceBranch the source branch
     * @param targetBranch the target branch
     * @return the SHA of the merge base, or null if an error occurred
     */
    private String getMergeBaseSha(String projectId, String sourceBranch, String targetBranch) {
        logger.debug("Getting merge base SHA between {} and {}", sourceBranch, targetBranch);
        String url = config.getGitlabUrl() + API_V_4_PROJECTS + projectId + "/repository/merge_base?refs[]=" +
                     encodeBranchName(sourceBranch) + "&refs[]=" + encodeBranchName(targetBranch);
        Request request = new Request.Builder()
                .url(url)
                .header(PRIVATE_TOKEN, config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to get merge base: {}", response);
                return null;
            }
            JsonNode mergeBaseNode = objectMapper.readTree(response.body().string());
            return mergeBaseNode.get("id").asText();
        } catch (IOException e) {
            logger.error("Error getting merge base SHA", e);
            return null;
        }
    }

    // Простий клас Project
    public static class Project {
        private int id;
        private String name;
        private String path;
        private String namespacePath; // Path to the namespace (group/subgroup)

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getNamespacePath() { return namespacePath; }
        public void setNamespacePath(String namespacePath) { this.namespacePath = namespacePath; }

        /**
         * Returns the project identifier in "subgroup/name" format
         * @return String in format "subgroup/name"
         */
        public String getPathName() { 
            // If namespacePath is available, extract the subgroup from it
            if (namespacePath != null && !namespacePath.isEmpty()) {
                // Extract the last part of the namespace path (the subgroup)
                int lastSlashIndex = namespacePath.lastIndexOf('/');
                if (lastSlashIndex >= 0) {
                    // There is a slash, so extract the subgroup (last part of the namespace path)
                    String subgroup = namespacePath.substring(lastSlashIndex + 1);
                    return subgroup + "/" + name;
                }
                // No slash, so the namespace path is the subgroup itself
                return namespacePath + "/" + name;
            }
            // Fallback to the old behavior if namespacePath is not available
            return name;
        }
    }

    /**
     * Encodes a branch name for use in GitLab API URLs.
     * This method handles special characters like slashes that need to be properly encoded.
     *
     * @param branchName the branch name to encode
     * @return the encoded branch name
     */
    private String encodeBranchName(String branchName) {
        if (branchName == null) {
            return null;
        }
        // Replace commas with URL-encoded representation before encoding the entire string
        // This ensures commas are properly handled in GitLab API requests
        String preprocessed = branchName.replace(",", "%2C");
        return URLEncoder.encode(preprocessed, StandardCharsets.UTF_8);
    }
}
