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

public class GitlaberApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GitlaberApp.class);

    private MainController controller;

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting application");

        // Завантаження налаштувань
        AppConfig config = AppConfig.load();

        // Завантаження головного вікна
        FXMLLoader fxmlLoader = new FXMLLoader(GitlaberApp.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Налаштування головного вікна
        stage.setTitle("GitLaberFX");
        stage.setScene(scene);

        // Ініціалізація контролера
        controller = fxmlLoader.getController();
        controller.initialize(config, stage);

        // Add a close request handler to ensure proper cleanup
        stage.setOnCloseRequest(event -> {
            controller.shutdown();
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
    }

    @Override
    public void stop() {
        logger.info("JavaFX stop method called");
        if (controller != null) {
            controller.shutdownExecutor();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
