package com.st.cloud.gametool;

import com.st.cloud.gametool.controller.AppController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * @author dev03
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("app-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1060, 600);
        stage.setTitle(" Spin 数 值 测 试 工 具   V1.0.0");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        //关闭触发
        AppController controller = fxmlLoader.getController();
        stage.setOnCloseRequest(event -> {
            controller.saveConfig();
            controller.close();
        });

        stage.show();
    }

    public void run(String[] args) {
        launch();
    }
}