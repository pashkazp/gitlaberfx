package com.depavlo.gitlaberfx.service;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.model.BranchModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitLabService {
    private static final Logger logger = LoggerFactory.getLogger(GitLabService.class);
    private final AppConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitLabService(AppConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public void connect() throws IOException {
        logger.info("Connecting to GitLab at {}", config.getGitlabUrl());
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new IOException("API key is not set");
        }
        // Тест з'єднання - спробуємо отримати список проєктів
        getProjects();
    }

    public List<Project> getProjects() throws IOException {
        logger.debug("Getting projects list");
        String url = config.getGitlabUrl() + "/api/v4/projects";
        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get projects: " + response);
            }

            JsonNode jsonArray = objectMapper.readTree(response.body().string());
            List<Project> projects = new ArrayList<>();
            for (JsonNode projectNode : jsonArray) {
                Project project = new Project();
                project.setId(projectNode.get("id").asInt());
                project.setName(projectNode.get("name").asText());
                project.setPath(projectNode.get("path").asText());
                projects.add(project);
            }
            return projects;
        }
    }

    public List<BranchModel> getBranches(String projectId) throws IOException {
        logger.debug("Getting branches for project {}", projectId);
        String url = config.getGitlabUrl() + "/api/v4/projects/" + projectId + "/repository/branches";
        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get branches: " + response);
            }

            JsonNode jsonArray = objectMapper.readTree(response.body().string());
            List<BranchModel> branches = new ArrayList<>();
            for (JsonNode branchNode : jsonArray) {
                String branchName = branchNode.get("name").asText();
                String lastCommitDate = branchNode.get("commit").get("committed_date").asText();
                boolean merged = isMerged(projectId, branchName);
                
                BranchModel branch = new BranchModel(branchName, lastCommitDate, merged);
                branches.add(branch);
            }
            return branches;
        }
    }

    public void deleteBranch(String projectId, String branchName) throws IOException {
        logger.info("Deleting branch {} from project {}", branchName, projectId);
        String url = config.getGitlabUrl() + "/api/v4/projects/" + projectId + "/repository/branches/" + branchName;
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .header("PRIVATE-TOKEN", config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to delete branch: " + response);
            }
        }
    }

    private boolean isMerged(String projectId, String branchName) {
        try {
            String url = config.getGitlabUrl() + "/api/v4/projects/" + projectId + "/merge_requests?state=merged&source_branch=" + branchName;
            Request request = new Request.Builder()
                    .url(url)
                    .header("PRIVATE-TOKEN", config.getApiKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonNode jsonArray = objectMapper.readTree(response.body().string());
                    return jsonArray.size() > 0;
                }
            }
        } catch (IOException e) {
            logger.error("Error checking if branch {} is merged", branchName, e);
        }
        return false;
    }

    public boolean isCommitInMainBranch(String projectId, String branchName, String mainBranch) throws IOException {
        logger.debug("Checking if branch {} is merged into {}", branchName, mainBranch);
        // Перевіряємо через merge requests API
        String url = config.getGitlabUrl() + "/api/v4/projects/" + projectId + "/merge_requests?state=merged&source_branch=" + branchName + "&target_branch=" + mainBranch;
        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", config.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to check merged status: " + response);
            }

            JsonNode jsonArray = objectMapper.readTree(response.body().string());
            return jsonArray.size() > 0;
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
} 