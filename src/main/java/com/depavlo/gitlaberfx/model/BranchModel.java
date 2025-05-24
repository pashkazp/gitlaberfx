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

    public BranchModel(String name, String lastCommit, boolean merged) {
        this.selected = new SimpleBooleanProperty(false);
        this.name = new SimpleStringProperty(name);
        this.lastCommit = new SimpleStringProperty(lastCommit);
        this.merged = new SimpleBooleanProperty(merged);
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
} 