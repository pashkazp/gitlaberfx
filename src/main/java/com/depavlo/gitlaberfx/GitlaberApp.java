package com.depavlo.gitlaberfx;

import com.depavlo.gitlaberfx.config.AppConfig;
import com.depavlo.gitlaberfx.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GitlaberApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GitlaberApp.class);

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
        MainController controller = fxmlLoader.getController();
        controller.initialize(config, stage);
        
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}