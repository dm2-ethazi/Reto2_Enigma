package com.enigma.javafx;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
        // Los estilos se mantienen separados del diseño declarativo de la vista.
        scene.getStylesheets().add(
                MainApplication.class.getResource("/com/enigma/javafx/styles.css").toExternalForm());

        // Asocia la escena a la ventana y la muestra al usuario.
        stage.setTitle("Gestor Enigma AES");
        stage.setScene(scene);
        scene.getRoot().setOpacity(0);
        stage.show();

        FadeTransition openingFade = new FadeTransition(OPENING_FADE_DURATION, scene.getRoot());
        openingFade.setFromValue(0);
        openingFade.setToValue(1);
        openingFade.play();
    }
}
