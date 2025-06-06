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
package com.depavlo.gitlaberfx.model;

import com.depavlo.gitlaberfx.service.GitLabService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * A model to hold the application's UI state centrally.
 * This class uses JavaFX properties to allow UI components to bind to the state and update automatically.
 */
public class UIStateModel {

    private final StringProperty currentProjectId = new SimpleStringProperty();
    private final StringProperty currentProjectName = new SimpleStringProperty();
    private final StringProperty currentTargetBranchName = new SimpleStringProperty();
    private final StringProperty statusMessage = new SimpleStringProperty();

    private final ObservableList<GitLabService.Project> allProjects = FXCollections.observableArrayList();
    private final ObservableList<BranchModel> currentProjectBranches = FXCollections.observableArrayList();

    public String getCurrentProjectId() { return currentProjectId.get(); }
    public void setCurrentProjectId(String id) { this.currentProjectId.set(id); }
    public StringProperty currentProjectIdProperty() { return currentProjectId; }

    public String getCurrentProjectName() { return currentProjectName.get(); }
    public void setCurrentProjectName(String name) { this.currentProjectName.set(name); }
    public StringProperty currentProjectNameProperty() { return currentProjectName; }

    public String getCurrentTargetBranchName() { return currentTargetBranchName.get(); }
    public void setCurrentTargetBranchName(String name) { this.currentTargetBranchName.set(name); }
    public StringProperty currentTargetBranchNameProperty() { return currentTargetBranchName; }

    public String getStatusMessage() { return statusMessage.get(); }
    public void setStatusMessage(String message) { this.statusMessage.set(message); }
    public StringProperty statusMessageProperty() { return statusMessage; }

    public ObservableList<GitLabService.Project> getAllProjects() { return allProjects; }
    public void setAllProjects(List<GitLabService.Project> projects) {
        this.allProjects.setAll(projects);
    }

    public ObservableList<BranchModel> getCurrentProjectBranches() { return currentProjectBranches; }
    public void setCurrentProjectBranches(List<BranchModel> branches) {
        this.currentProjectBranches.setAll(branches);
    }

    public void clearProjectBranches() {
        this.currentProjectBranches.clear();
    }
}