package com.enigma.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApplication.class.getResource("/com/enigma/javafx/main-view.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                MainApplication.class.getResource("/com/enigma/javafx/styles.css").toExternalForm());

        stage.setTitle("Enigma");
        stage.setScene(scene);
        stage.show();
    }
}
