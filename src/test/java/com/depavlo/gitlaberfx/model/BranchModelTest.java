package com.depavlo.gitlaberfx.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BranchModelTest {

    @Test
    void testConstructor() {
        BranchModel branch = new BranchModel("test-branch", "2024-01-01", true);
        
        assertEquals("test-branch", branch.getName());
        assertEquals("2024-01-01", branch.getLastCommit());
        assertTrue(branch.isMerged());
        assertFalse(branch.isSelected());
    }

    @Test
    void testProperties() {
        BranchModel branch = new BranchModel("test-branch", "2024-01-01", true);
        
        // Перевірка властивостей
        StringProperty nameProperty = branch.nameProperty();
        StringProperty lastCommitProperty = branch.lastCommitProperty();
        BooleanProperty mergedProperty = branch.mergedProperty();
        BooleanProperty selectedProperty = branch.selectedProperty();
        
        assertNotNull(nameProperty);
        assertNotNull(lastCommitProperty);
        assertNotNull(mergedProperty);
        assertNotNull(selectedProperty);
        
        // Перевірка зміни значень
        branch.setName("new-name");
        assertEquals("new-name", branch.getName());
        
        branch.setLastCommit("2024-01-02");
        assertEquals("2024-01-02", branch.getLastCommit());
        
        branch.setMerged(false);
        assertFalse(branch.isMerged());
        
        branch.setSelected(true);
        assertTrue(branch.isSelected());
    }
} 