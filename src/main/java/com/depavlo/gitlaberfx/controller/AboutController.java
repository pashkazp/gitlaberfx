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
package com.depavlo.gitlaberfx.controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the About dialog.
 * This class handles the functionality of the About dialog, which displays
 * information about the application such as version, author, and license.
 */
public class AboutController {
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(AboutController.class);

    /** The stage that contains the About dialog. */
    private Stage stage;

    /**
     * Initializes the controller with the stage that contains the About dialog.
     * This method is called after the FXML has been loaded.
     *
     * @param stage The stage that contains the About dialog
     */
    public void initialize(Stage stage) {
        logger.debug("initialize: stage={}", stage != null ? "not null" : "null");
        this.stage = stage;
    }

    /**
     * Closes the About dialog.
     * This method is called when the user clicks the close button.
     */
    @FXML
    private void close() {
        logger.debug("Closing about dialog");
        stage.close();
    }
} 
