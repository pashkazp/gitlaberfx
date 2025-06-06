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
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ResourceBundle;

public class GitlaberApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GitlaberApp.class);

    private MainController controller;

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting application");

        // Завантаження налаштувань
        AppConfig config = AppConfig.load();

        // Set the application locale from config
        String localeCode = config.getLocale();
        if (localeCode != null && !localeCode.isEmpty()) {
            String[] localeParts = localeCode.split("_");
            if (localeParts.length == 2) {
                com.depavlo.gitlaberfx.util.I18nUtil.setLocale(new java.util.Locale(localeParts[0], localeParts[1]));
            }
        } else {
            // Default to English if no locale is specified
            com.depavlo.gitlaberfx.util.I18nUtil.setLocale(new java.util.Locale("en", "US"));
        }

        // Завантаження головного вікна
        FXMLLoader fxmlLoader = new FXMLLoader(GitlaberApp.class.getResource("/fxml/main.fxml"));
        fxmlLoader.setResources(ResourceBundle.getBundle("i18n.messages", com.depavlo.gitlaberfx.util.I18nUtil.getCurrentLocale()));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Налаштування головного вікна
        stage.setTitle(com.depavlo.gitlaberfx.util.I18nUtil.getMessage("app.title"));
        stage.setScene(scene);

        // Ініціалізація контролера
        controller = fxmlLoader.getController();
        controller.initialize(config, stage);

        // Add a close request handler to ensure proper cleanup
        stage.setOnCloseRequest(event -> {
            logger.info("Close request received");
            controller.shutdown();

            // Consume the event to prevent default handling
            event.consume();

            // Force exit to ensure all threads are terminated
            System.exit(0);
        });

        // Add a shutdown hook to ensure background tasks are terminated
        // when the application exits abnormally
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutdown hook triggered");
            if (controller != null) {
                // Run on JavaFX thread if possible, otherwise run directly
                if (Platform.isFxApplicationThread()) {
                    controller.shutdownExecutor();
                } else {
                    try {
                        Platform.runLater(controller::shutdownExecutor);
                        // Give a short time for the runLater to execute
                        Thread.sleep(200);
                    } catch (Exception e) {
                        // If Platform is already shutdown, call directly
                        controller.shutdownExecutor();
                    }
                }
            }
        }));

        stage.show();
        controller.refreshProjects();
    }

    @Override
    public void stop() {
        logger.info("JavaFX stop method called");
        if (controller != null) {
            controller.shutdownExecutor();
        }

        // Ensure all threads are terminated
        try {
            // Give a short time for any remaining tasks to complete
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // Ignore interruption
        }

        // Force exit to ensure all threads are terminated
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }
}
