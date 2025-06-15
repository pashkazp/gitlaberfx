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
import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BranchModelTest {

    @Test
    void testConstructor() {
        BranchModel branch = new BranchModel("test-branch", "2024-01-01", "abc123", true);

        assertEquals("test-branch", branch.getName());
        assertEquals("2024-01-01", branch.getLastCommit());
        assertEquals("abc123", branch.getLastCommitSha());
        assertTrue(branch.isMerged());
        assertFalse(branch.isSelected());
    }

    @Test
    void testProperties() {
        BranchModel branch = new BranchModel("test-branch", "2024-01-01", "abc123", true);

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
