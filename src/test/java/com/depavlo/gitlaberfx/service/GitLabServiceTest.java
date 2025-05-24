package com.depavlo.gitlaberfx.service;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.model.BranchModel;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GitLabServiceTest {

    @Mock
    private GitLabApi gitLabApi;

    private GitLabService gitLabService;
    private AppConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new AppConfig();
        config.setGitlabUrl("https://gitlab.com");
        config.setApiKey("test-api-key");
        gitLabService = new GitLabService(config);
    }

    @Test
    void testConnect() throws GitLabApiException {
        // Налаштування моку
        when(gitLabApi.getProjectApi().getProjects()).thenReturn(Arrays.asList(
                createTestProject(1, "project1"),
                createTestProject(2, "project2")
        ));

        // Тестування підключення
        gitLabService.connect();
        verify(gitLabApi).getProjectApi().getProjects();
    }

    @Test
    void testGetProjects() throws GitLabApiException {
        // Налаштування моку
        List<Project> projects = Arrays.asList(
                createTestProject(1, "project1"),
                createTestProject(2, "project2")
        );
        when(gitLabApi.getProjectApi().getProjects()).thenReturn(projects);

        // Тестування отримання проєктів
        List<Project> result = gitLabService.getProjects();
        assertEquals(2, result.size());
        assertEquals("project1", result.get(0).getName());
        assertEquals("project2", result.get(1).getName());
    }

    @Test
    void testGetBranches() throws GitLabApiException {
        // Налаштування моку
        String projectId = "1";
        List<Branch> branches = Arrays.asList(
                createTestBranch("branch1", "commit1"),
                createTestBranch("branch2", "commit2")
        );
        when(gitLabApi.getRepositoryApi().getBranches(projectId)).thenReturn(branches);

        // Тестування отримання гілок
        List<BranchModel> result = gitLabService.getBranches(projectId);
        assertEquals(2, result.size());
        assertEquals("branch1", result.get(0).getName());
        assertEquals("branch2", result.get(1).getName());
    }

    @Test
    void testDeleteBranch() throws GitLabApiException {
        // Налаштування моку
        String projectId = "1";
        String branchName = "test-branch";

        // Тестування видалення гілки
        gitLabService.deleteBranch(projectId, branchName);
        verify(gitLabApi.getRepositoryApi()).deleteBranch(projectId, branchName);
    }

    private Project createTestProject(int id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        return project;
    }

    private Branch createTestBranch(String name, String commitId) {
        Branch branch = new Branch();
        branch.setName(name);
        Commit commit = new Commit();
        commit.setId(commitId);
        commit.setCommittedDate(new Date());
        branch.setCommit(commit);
        return branch;
    }
} 