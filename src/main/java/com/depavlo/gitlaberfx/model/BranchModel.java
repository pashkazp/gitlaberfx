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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a GitLab branch with its properties and state.
 * This class uses JavaFX properties for data binding with the UI components.
 * It contains information about the branch such as name, last commit,
 * merge status, protection status, and other GitLab branch attributes.
 */
public class BranchModel {
    /** Property indicating whether this branch is selected in the UI. */
    private final BooleanProperty selected;

    /** Property containing the name of the branch. */
    private final StringProperty name;

    /** Property containing the date of the last commit on this branch in ISO 8601 format. */
    private final StringProperty lastCommit;

    /** SHA of the last commit on this branch. */
    private final String lastCommitSha;

    /** Property indicating whether this branch is merged. */
    private final BooleanProperty merged;

    /** Property indicating whether this branch is merged into the target branch. */
    private final BooleanProperty mergedIntoTarget;

    /** Property indicating whether this branch is protected in GitLab. */
    private final BooleanProperty protected_;

    /** Property indicating whether developers can push to this branch. */
    private final BooleanProperty developersCanPush;

    /** Property indicating whether developers can merge to this branch. */
    private final BooleanProperty developersCanMerge;

    /** Property indicating whether the current user can push to this branch. */
    private final BooleanProperty canPush;

    /** Property indicating whether this is the default branch of the repository. */
    private final BooleanProperty default_;

    /**
     * Constructs a new BranchModel with all properties.
     *
     * @param name               The name of the branch
     * @param lastCommit         The date of the last commit on this branch in ISO 8601 format
     * @param lastCommitSha      The SHA of the last commit on this branch
     * @param merged             Whether this branch is merged
     * @param protected_         Whether this branch is protected in GitLab
     * @param developersCanPush  Whether developers can push to this branch
     * @param developersCanMerge Whether developers can merge to this branch
     * @param canPush            Whether the current user can push to this branch
     * @param default_           Whether this is the default branch of the repository
     */
    public BranchModel(String name, String lastCommit, String lastCommitSha, boolean merged, boolean protected_, 
                  boolean developersCanPush, boolean developersCanMerge, boolean canPush, boolean default_) {
        this.selected = new SimpleBooleanProperty(false);
        this.name = new SimpleStringProperty(name);
        this.lastCommit = new SimpleStringProperty(lastCommit);
        this.lastCommitSha = lastCommitSha;
        this.merged = new SimpleBooleanProperty(merged);
        this.mergedIntoTarget = new SimpleBooleanProperty(false);
        this.protected_ = new SimpleBooleanProperty(protected_);
        this.developersCanPush = new SimpleBooleanProperty(developersCanPush);
        this.developersCanMerge = new SimpleBooleanProperty(developersCanMerge);
        this.canPush = new SimpleBooleanProperty(canPush);
        this.default_ = new SimpleBooleanProperty(default_);
    }

    /**
     * Constructs a new BranchModel with basic properties, setting all other properties to false.
     * This constructor is provided for backward compatibility.
     *
     * @param name         The name of the branch
     * @param lastCommit   The date of the last commit on this branch in ISO 8601 format
     * @param lastCommitSha The SHA of the last commit on this branch
     * @param merged       Whether this branch is merged
     */
    public BranchModel(String name, String lastCommit, String lastCommitSha, boolean merged) {
        this(name, lastCommit, lastCommitSha, merged, false, false, false, false, false);
    }

    /**
     * Returns whether this branch is selected in the UI.
     *
     * @return true if the branch is selected, false otherwise
     */
    public boolean isSelected() {
        return selected.get();
    }

    /**
     * Returns the property representing whether this branch is selected.
     * This can be used for binding to UI components.
     *
     * @return the selected property
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * Sets whether this branch is selected in the UI.
     * Protected branches cannot be selected.
     *
     * @param selected true to select the branch, false to deselect it
     */
    public void setSelected(boolean selected) {
        // Protected branches cannot be selected
        if (isProtected() && selected) {
            return;
        }
        this.selected.set(selected);
    }

    /**
     * Returns the name of this branch.
     *
     * @return the branch name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Returns the property representing the name of this branch.
     * This can be used for binding to UI components.
     *
     * @return the name property
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Sets the name of this branch.
     *
     * @param name the new branch name
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * Returns the date of the last commit on this branch in ISO 8601 format.
     *
     * @return the last commit date
     */
    public String getLastCommit() {
        return lastCommit.get();
    }

    /**
     * Returns the property representing the last commit date of this branch.
     * This can be used for binding to UI components.
     *
     * @return the last commit property
     */
    public StringProperty lastCommitProperty() {
        return lastCommit;
    }

    /**
     * Sets the date of the last commit on this branch in ISO 8601 format.
     *
     * @param lastCommit the new last commit date
     */
    public void setLastCommit(String lastCommit) {
        this.lastCommit.set(lastCommit);
    }

    /**
     * Returns the SHA of the last commit on this branch.
     *
     * @return the SHA of the last commit
     */
    public String getLastCommitSha() {
        return lastCommitSha;
    }

    /**
     * Returns whether this branch is merged.
     *
     * @return true if the branch is merged, false otherwise
     */
    public boolean isMerged() {
        return merged.get();
    }

    /**
     * Returns the property representing whether this branch is merged.
     * This can be used for binding to UI components.
     *
     * @return the merged property
     */
    public BooleanProperty mergedProperty() {
        return merged;
    }

    /**
     * Sets whether this branch is merged.
     *
     * @param merged true if the branch is merged, false otherwise
     */
    public void setMerged(boolean merged) {
        this.merged.set(merged);
    }

    /**
     * Returns whether this branch is merged into the target branch.
     *
     * @return true if the branch is merged into the target branch, false otherwise
     */
    public boolean isMergedIntoTarget() {
        return mergedIntoTarget.get();
    }

    /**
     * Returns the property representing whether this branch is merged into the target branch.
     * This can be used for binding to UI components.
     *
     * @return the mergedIntoTarget property
     */
    public BooleanProperty mergedIntoTargetProperty() {
        return mergedIntoTarget;
    }

    /**
     * Sets whether this branch is merged into the target branch.
     *
     * @param mergedIntoTarget true if the branch is merged into the target branch, false otherwise
     */
    public void setMergedIntoTarget(boolean mergedIntoTarget) {
        this.mergedIntoTarget.set(mergedIntoTarget);
    }

    /**
     * Returns whether this branch is protected in GitLab.
     *
     * @return true if the branch is protected, false otherwise
     */
    public boolean isProtected() {
        return protected_.get();
    }

    /**
     * Returns the property representing whether this branch is protected in GitLab.
     * This can be used for binding to UI components.
     *
     * @return the protected property
     */
    public BooleanProperty protectedProperty() {
        return protected_;
    }

    /**
     * Sets whether this branch is protected in GitLab.
     *
     * @param protected_ true if the branch is protected, false otherwise
     */
    public void setProtected(boolean protected_) {
        this.protected_.set(protected_);
    }

    /**
     * Returns whether developers can push to this branch.
     *
     * @return true if developers can push to this branch, false otherwise
     */
    public boolean isDevelopersCanPush() {
        return developersCanPush.get();
    }

    /**
     * Returns the property representing whether developers can push to this branch.
     * This can be used for binding to UI components.
     *
     * @return the developersCanPush property
     */
    public BooleanProperty developersCanPushProperty() {
        return developersCanPush;
    }

    /**
     * Sets whether developers can push to this branch.
     *
     * @param developersCanPush true if developers can push to this branch, false otherwise
     */
    public void setDevelopersCanPush(boolean developersCanPush) {
        this.developersCanPush.set(developersCanPush);
    }

    /**
     * Returns whether developers can merge to this branch.
     *
     * @return true if developers can merge to this branch, false otherwise
     */
    public boolean isDevelopersCanMerge() {
        return developersCanMerge.get();
    }

    /**
     * Returns the property representing whether developers can merge to this branch.
     * This can be used for binding to UI components.
     *
     * @return the developersCanMerge property
     */
    public BooleanProperty developersCanMergeProperty() {
        return developersCanMerge;
    }

    /**
     * Sets whether developers can merge to this branch.
     *
     * @param developersCanMerge true if developers can merge to this branch, false otherwise
     */
    public void setDevelopersCanMerge(boolean developersCanMerge) {
        this.developersCanMerge.set(developersCanMerge);
    }

    /**
     * Returns whether the current user can push to this branch.
     *
     * @return true if the current user can push to this branch, false otherwise
     */
    public boolean isCanPush() {
        return canPush.get();
    }

    /**
     * Returns the property representing whether the current user can push to this branch.
     * This can be used for binding to UI components.
     *
     * @return the canPush property
     */
    public BooleanProperty canPushProperty() {
        return canPush;
    }

    /**
     * Sets whether the current user can push to this branch.
     *
     * @param canPush true if the current user can push to this branch, false otherwise
     */
    public void setCanPush(boolean canPush) {
        this.canPush.set(canPush);
    }

    /**
     * Returns whether this is the default branch of the repository.
     *
     * @return true if this is the default branch, false otherwise
     */
    public boolean isDefault() {
        return default_.get();
    }

    /**
     * Returns the property representing whether this is the default branch of the repository.
     * This can be used for binding to UI components.
     *
     * @return the default property
     */
    public BooleanProperty defaultProperty() {
        return default_;
    }

    /**
     * Sets whether this is the default branch of the repository.
     *
     * @param default_ true if this is the default branch, false otherwise
     */
    public void setDefault(boolean default_) {
        this.default_.set(default_);
    }
}
