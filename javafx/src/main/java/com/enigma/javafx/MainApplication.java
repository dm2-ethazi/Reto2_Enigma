package com.enigma.javafx;

import com.enigma.javafx.controller.MainController;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Configura y muestra la ventana principal de Gestor Enigma AES.
 *
 * <p>La estructura de la interfaz se carga desde FXML y su apariencia desde
 * una hoja CSS externa.</p>
 */
public class MainApplication extends Application {
    private static final double PREFERRED_WINDOW_WIDTH = 1000.0;
    private static final double PREFERRED_WINDOW_HEIGHT = 720.0;
    private static final double MAX_SCREEN_WIDTH_RATIO = 0.90;
    private static final double MAX_SCREEN_HEIGHT_RATIO = 0.85;
    private static final Duration OPENING_FADE_DURATION = Duration.millis(220);

    /**
     * Carga la vista, aplica los estilos y presenta la ventana principal.
     *
     * @param stage ventana principal proporcionada por JavaFX
     * @throws IOException si no se puede leer el FXML o la hoja de estilos
     */
    @Override
    public void start(Stage stage) throws IOException {
        // FXMLLoader crea los nodos de la interfaz e instancia su controlador.
        FXMLLoader loader = new FXMLLoader(
                MainApplication.class.getResource("/com/enigma/javafx/main-view.fxml"));
        Scene scene = new Scene(loader.load());
        MainController controller = loader.getController();
        controller.installKeyboardShortcuts(scene);
        // Los estilos se mantienen separados del diseño declarativo de la vista.
        scene.getStylesheets().add(
                MainApplication.class.getResource("/com/enigma/javafx/styles.css").toExternalForm());

        // Asocia la escena a la ventana y la muestra al usuario.
        stage.setTitle("Gestor Enigma AES");
        stage.setScene(scene);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double windowWidth = Math.min(
                PREFERRED_WINDOW_WIDTH, screenBounds.getWidth() * MAX_SCREEN_WIDTH_RATIO);
        double windowHeight = Math.min(
                PREFERRED_WINDOW_HEIGHT, screenBounds.getHeight() * MAX_SCREEN_HEIGHT_RATIO);
        stage.setMaximized(false);
        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);
        stage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - windowWidth) / 2);
        stage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - windowHeight) / 2);
        scene.getRoot().setOpacity(0);
        stage.show();

        FadeTransition openingFade = new FadeTransition(OPENING_FADE_DURATION, scene.getRoot());
        openingFade.setFromValue(0);
        openingFade.setToValue(1);
        openingFade.play();
    }
}
