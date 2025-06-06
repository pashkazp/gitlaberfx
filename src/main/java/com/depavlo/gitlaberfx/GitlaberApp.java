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
package com.depavlo.gitlaberfx;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.controller.MainController;
import com.depavlo.gitlaberfx.util.I18nUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class GitlaberApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GitlaberApp.class);

    private MainController controller;

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting application");

        AppConfig config = AppConfig.load();

        String localeCode = config.getLocale();
        if (localeCode != null && !localeCode.isEmpty()) {
            String[] localeParts = localeCode.replace('-', '_').split("_");
            if (localeParts.length == 2) {
                com.depavlo.gitlaberfx.util.I18nUtil.setLocale(new Locale(localeParts[0], localeParts[1]));
            }
        } else {
            com.depavlo.gitlaberfx.util.I18nUtil.setLocale(new Locale("en", "US"));
        }

        FXMLLoader fxmlLoader = new FXMLLoader(GitlaberApp.class.getResource("/fxml/main.fxml"));
        fxmlLoader.setResources(ResourceBundle.getBundle("i18n.messages", com.depavlo.gitlaberfx.util.I18nUtil.getCurrentLocale()));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        stage.setTitle(I18nUtil.getMessage("app.title"));
        stage.setScene(scene);

        controller = fxmlLoader.getController();
        controller.initialize(config, stage);

        stage.setOnCloseRequest(event -> {
            logger.info("Stage is closing, shutting down controller.");
            controller.shutdown();
        });

        stage.show();

        // Trigger the initial data load after the UI is visible.
        controller.startInitialLoad();
    }

    @Override
    public void stop() {
        logger.info("JavaFX stop method called. Ensuring controller is shutdown.");
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}