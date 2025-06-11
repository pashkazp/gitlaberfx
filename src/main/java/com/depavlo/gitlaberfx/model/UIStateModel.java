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
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

    /** Property containing the ID of the currently selected project. */
    private final StringProperty currentProjectId = new SimpleStringProperty();

    /** Property containing the name of the currently selected project. */
    private final StringProperty currentProjectName = new SimpleStringProperty();

    /** Property containing the name of the currently selected target branch. */
    private final StringProperty currentTargetBranchName = new SimpleStringProperty();

    /** Property containing the current status message to display in the UI. */
    private final StringProperty statusMessage = new SimpleStringProperty();

    /** Property indicating whether the application is currently busy with a background task. */
    private final BooleanProperty isBusy = new SimpleBooleanProperty(false);

    /** Observable list of all GitLab projects available to the user. */
    private final ObservableList<GitLabService.Project> allProjects = FXCollections.observableArrayList();

    /** 
     * Observable list of branches for the currently selected project.
     * This list is configured to trigger updates when the mergedIntoTarget or selected properties change.
     */
    private final ObservableList<BranchModel> currentProjectBranches =
            FXCollections.observableArrayList(branch -> new Observable[] {
                    branch.mergedIntoTargetProperty(),
                    branch.selectedProperty(),
                    branch.nameProperty()
            });

    /**
     * Returns the ID of the currently selected project.
     *
     * @return the current project ID
     */
    public String getCurrentProjectId() { return currentProjectId.get(); }

    /**
     * Sets the ID of the currently selected project.
     *
     * @param id the project ID to set
     */
    public void setCurrentProjectId(String id) { this.currentProjectId.set(id); }

    /**
     * Returns the property representing the ID of the currently selected project.
     * This can be used for binding to UI components.
     *
     * @return the current project ID property
     */
    public StringProperty currentProjectIdProperty() { return currentProjectId; }

    /**
     * Returns the name of the currently selected project.
     *
     * @return the current project name
     */
    public String getCurrentProjectName() { return currentProjectName.get(); }

    /**
     * Sets the name of the currently selected project.
     *
     * @param name the project name to set
     */
    public void setCurrentProjectName(String name) { this.currentProjectName.set(name); }

    /**
     * Returns the property representing the name of the currently selected project.
     * This can be used for binding to UI components.
     *
     * @return the current project name property
     */
    public StringProperty currentProjectNameProperty() { return currentProjectName; }

    /**
     * Returns the name of the currently selected target branch.
     *
     * @return the current target branch name
     */
    public String getCurrentTargetBranchName() { return currentTargetBranchName.get(); }

    /**
     * Sets the name of the currently selected target branch.
     *
     * @param name the target branch name to set
     */
    public void setCurrentTargetBranchName(String name) { this.currentTargetBranchName.set(name); }

    /**
     * Returns the property representing the name of the currently selected target branch.
     * This can be used for binding to UI components.
     *
     * @return the current target branch name property
     */
    public StringProperty currentTargetBranchNameProperty() { return currentTargetBranchName; }

    /**
     * Returns the current status message to display in the UI.
     *
     * @return the current status message
     */
    public String getStatusMessage() { return statusMessage.get(); }

    /**
     * Sets the current status message to display in the UI.
     *
     * @param message the status message to set
     */
    public void setStatusMessage(String message) { this.statusMessage.set(message); }

    /**
     * Returns the property representing the current status message.
     * This can be used for binding to UI components.
     *
     * @return the status message property
     */
    public StringProperty statusMessageProperty() { return statusMessage; }

    /**
     * Returns whether the application is currently busy with a background task.
     *
     * @return true if the application is busy, false otherwise
     */
    public boolean isBusy() { return isBusy.get(); }

    /**
     * Sets whether the application is currently busy with a background task.
     *
     * @param busy true if the application is busy, false otherwise
     */
    public void setBusy(boolean busy) { this.isBusy.set(busy); }

    /**
     * Returns the property representing whether the application is currently busy.
     * This can be used for binding to UI components.
     *
     * @return the busy property
     */
    public BooleanProperty busyProperty() { return isBusy; }

    /**
     * Returns the observable list of all GitLab projects available to the user.
     *
     * @return the list of all projects
     */
    public ObservableList<GitLabService.Project> getAllProjects() { return allProjects; }

    /**
     * Sets the list of all GitLab projects available to the user.
     *
     * @param projects the list of projects to set
     */
    public void setAllProjects(List<GitLabService.Project> projects) {
        this.allProjects.setAll(projects);
    }

    /**
     * Returns the observable list of branches for the currently selected project.
     *
     * @return the list of branches for the current project
     */
    public ObservableList<BranchModel> getCurrentProjectBranches() { return currentProjectBranches; }

    /**
     * Sets the list of branches for the currently selected project.
     *
     * @param branches the list of branches to set
     */
    public void setCurrentProjectBranches(List<BranchModel> branches) {
        this.currentProjectBranches.setAll(branches);
    }

    /**
     * Clears the list of branches for the currently selected project.
     */
    public void clearProjectBranches() {
        this.currentProjectBranches.clear();
    }
}
