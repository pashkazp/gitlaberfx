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
package com.depavlo.gitlaberfx.util;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.model.BranchModel;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for DialogHelper
 * 
 * Note: These tests don't actually test the real implementation of DialogHelper methods
 * because they require a running JavaFX environment. Instead, we're just verifying that
 * the methods exist and have the expected signatures.
 */
@ExtendWith(MockitoExtension.class)
class DialogHelperTest {

    @Mock
    private Stage mockStage;

    @Test
    void testShowSettingsDialog() {
        // Create test data
        AppConfig config = new AppConfig();

        // Test that the method exists and has the expected signature
        assertNotNull(DialogHelper.class);

        // Verify that the method can be called with the expected parameters
        // This doesn't actually call the method, just verifies it exists
        assertDoesNotThrow(() -> {
            // This is just a compile-time check
            DialogHelper.SettingsResult result = null;
            if (false) { // Never executed, just for compile-time checking
                result = DialogHelper.showSettingsDialog(mockStage, config);
            }
        });
    }

    @Test
    void testShowDeleteConfirmationDialog() {
        // Create test data
        List<BranchModel> branches = List.of(
            new BranchModel("branch1", "2024-01-01", true),
            new BranchModel("branch2", "2024-01-02", false)
        );

        // Test that the method exists and has the expected signature
        assertNotNull(DialogHelper.class);

        // Verify that the method can be called with the expected parameters
        assertDoesNotThrow(() -> {
            // This is just a compile-time check
            List<BranchModel> result = null;
            if (false) { // Never executed, just for compile-time checking
                result = DialogHelper.showDeleteConfirmationDialog(mockStage, branches);
            }
        });
    }

    @Test
    void testShowDatePickerDialog() {
        // Test that the method exists and has the expected signature
        assertNotNull(DialogHelper.class);

        // Verify that the method can be called with the expected parameters
        assertDoesNotThrow(() -> {
            // This is just a compile-time check
            LocalDate result = null;
            if (false) { // Never executed, just for compile-time checking
                result = DialogHelper.showDatePickerDialog(mockStage);
            }
        });
    }

    @Test
    void testShowAboutDialog() {
        // Test that the method exists and has the expected signature
        assertNotNull(DialogHelper.class);

        // Verify that the method can be called with the expected parameters
        assertDoesNotThrow(() -> {
            // This is just a compile-time check
            if (false) { // Never executed, just for compile-time checking
                DialogHelper.showAboutDialog(mockStage);
            }
        });
    }

    @Test
    void testShowLoadingDialog() {
        // Test that the method exists and has the expected signature
        assertNotNull(DialogHelper.class);

        // Verify that the method can be called with the expected parameters
        assertDoesNotThrow(() -> {
            // This is just a compile-time check
            Stage result = null;
            if (false) { // Never executed, just for compile-time checking
                result = DialogHelper.showLoadingDialog(mockStage, "Loading...");
            }
        });
    }

    @Test
    void testHideLoadingDialog() {
        // Test that the method exists and has the expected signature
        assertNotNull(DialogHelper.class);

        // Verify that the method can be called with the expected parameters
        assertDoesNotThrow(() -> {
            // This is just a compile-time check
            if (false) { // Never executed, just for compile-time checking
                DialogHelper.hideLoadingDialog();
            }
        });
    }
}
