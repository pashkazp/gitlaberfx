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
                    boolean merged = isMerged(projectId, branchName);
                    branches.add(new BranchModel(branchName, lastCommitDate, merged));
                }
                if (jsonArray.size() < perPage) break;
                page++;
            }
        }
        return branches;
    }

    public void deleteBranch(String projectId, String branchName) throws IOException {
        logger.info("Deleting branch {} from project {}", branchName, projectId);
        String url = config.getGitlabUrl() + API_V_4_PROJECTS + projectId + "/repository/branches/" + branchName;
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

    private boolean isMerged(String projectId, String branchName) {
        int page = 1;
        int perPage = 100;
        try {
            while (true) {
                String url = config.getGitlabUrl() + API_V_4_PROJECTS + projectId +
                        "/merge_requests?state=merged&source_branch=" + branchName +
                        "&per_page=" + perPage + "&page=" + page;
                Request request = new Request.Builder()
                        .url(url)
                        .header(PRIVATE_TOKEN, config.getApiKey())
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        break;
                    }
                    JsonNode jsonArray = objectMapper.readTree(response.body().string());
                    if (!jsonArray.isArray() || jsonArray.size() == 0) break;
                    if (jsonArray.size() > 0) return true;
                    if (jsonArray.size() < perPage) break;
                    page++;
                }
            }
        } catch (IOException e) {
            logger.error("Error checking if branch {} is merged", branchName, e);
        }
        return false;
    }

    public boolean isCommitInMainBranch(String projectId, String branchName, String mainBranch) throws IOException {
        logger.debug("Checking if branch {} is merged into {}", branchName, mainBranch);
        int page = 1;
        int perPage = 100;
        while (true) {
            String url = config.getGitlabUrl() + API_V_4_PROJECTS + projectId +
                    "/merge_requests?state=merged&source_branch=" + branchName +
                    "&target_branch=" + mainBranch +
                    "&per_page=" + perPage + "&page=" + page;
            Request request = new Request.Builder()
                    .url(url)
                    .header(PRIVATE_TOKEN, config.getApiKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to check merged status: " + response);
                }
                JsonNode jsonArray = objectMapper.readTree(response.body().string());
                if (!jsonArray.isArray() || jsonArray.size() == 0) break;
                if (jsonArray.size() > 0) return true;
                if (jsonArray.size() < perPage) break;
                page++;
            }
        }
        return false;
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
} 
