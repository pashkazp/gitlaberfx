package com.depavlo.gitlaberfx.util;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.model.BranchModel;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DialogHelperTest extends ApplicationTest {

    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
    }

    @BeforeEach
    void setUp() {
        // Ініціалізація JavaFX середовища
    }

    @Test
    void testShowSettingsDialog() {
        AppConfig config = new AppConfig();
        boolean result = DialogHelper.showSettingsDialog(stage, config);
        assertFalse(result); // За замовчуванням повертає false, оскільки діалог закривається
    }

    @Test
    void testShowDeleteConfirmationDialog() {
        List<BranchModel> branches = List.of(
            new BranchModel("branch1", "2024-01-01", true),
            new BranchModel("branch2", "2024-01-02", false)
        );
        
        List<BranchModel> result = DialogHelper.showDeleteConfirmationDialog(stage, branches);
        assertNull(result); // За замовчуванням повертає null, оскільки діалог закривається
    }

    @Test
    void testShowDatePickerDialog() {
        LocalDate result = DialogHelper.showDatePickerDialog(stage);
        assertNull(result); // За замовчуванням повертає null, оскільки діалог закривається
    }

    @Test
    void testShowAboutDialog() {
        // Перевіряємо, що діалог не викидає винятків
        assertDoesNotThrow(() -> DialogHelper.showAboutDialog(stage));
    }
} 