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

public class BranchModel {
    private final BooleanProperty selected;
    private final StringProperty name;
    private final StringProperty lastCommit;
    private final BooleanProperty merged;
    private final BooleanProperty protected_;
    private final BooleanProperty developersCanPush;
    private final BooleanProperty developersCanMerge;
    private final BooleanProperty canPush;
    private final BooleanProperty default_;

    public BranchModel(String name, String lastCommit, boolean merged, boolean protected_, 
                  boolean developersCanPush, boolean developersCanMerge, boolean canPush, boolean default_) {
        this.selected = new SimpleBooleanProperty(false);
        this.name = new SimpleStringProperty(name);
        this.lastCommit = new SimpleStringProperty(lastCommit);
        this.merged = new SimpleBooleanProperty(merged);
        this.protected_ = new SimpleBooleanProperty(protected_);
        this.developersCanPush = new SimpleBooleanProperty(developersCanPush);
        this.developersCanMerge = new SimpleBooleanProperty(developersCanMerge);
        this.canPush = new SimpleBooleanProperty(canPush);
        this.default_ = new SimpleBooleanProperty(default_);
    }

    // Constructor with default values for backward compatibility
    public BranchModel(String name, String lastCommit, boolean merged) {
        this(name, lastCommit, merged, false, false, false, false, false);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getLastCommit() {
        return lastCommit.get();
    }

    public StringProperty lastCommitProperty() {
        return lastCommit;
    }

    public void setLastCommit(String lastCommit) {
        this.lastCommit.set(lastCommit);
    }

    public boolean isMerged() {
        return merged.get();
    }

    public BooleanProperty mergedProperty() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged.set(merged);
    }

    public boolean isProtected() {
        return protected_.get();
    }

    public BooleanProperty protectedProperty() {
        return protected_;
    }

    public void setProtected(boolean protected_) {
        this.protected_.set(protected_);
    }

    public boolean isDevelopersCanPush() {
        return developersCanPush.get();
    }

    public BooleanProperty developersCanPushProperty() {
        return developersCanPush;
    }

    public void setDevelopersCanPush(boolean developersCanPush) {
        this.developersCanPush.set(developersCanPush);
    }

    public boolean isDevelopersCanMerge() {
        return developersCanMerge.get();
    }

    public BooleanProperty developersCanMergeProperty() {
        return developersCanMerge;
    }

    public void setDevelopersCanMerge(boolean developersCanMerge) {
        this.developersCanMerge.set(developersCanMerge);
    }

    public boolean isCanPush() {
        return canPush.get();
    }

    public BooleanProperty canPushProperty() {
        return canPush;
    }

    public void setCanPush(boolean canPush) {
        this.canPush.set(canPush);
    }

    public boolean isDefault() {
        return default_.get();
    }

    public BooleanProperty defaultProperty() {
        return default_;
    }

    public void setDefault(boolean default_) {
        this.default_.set(default_);
    }
}
