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
        // Тест з'єднання - спробуємо отримати список проєктів
        getProjects();
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
                    // Initialize merged flag to false, it will be updated when a main branch is selected
                    branches.add(new BranchModel(branchName, lastCommitDate, false));
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

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
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
        return URLEncoder.encode(branchName, StandardCharsets.UTF_8);
    }
}
