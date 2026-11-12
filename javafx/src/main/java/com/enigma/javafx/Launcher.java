package com.enigma.javafx;

import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada estable de la aplicación.
 *
 * <p>
 * Se separa de {@link MainApplication} para que el lanzador de Java no
 * tenga que resolver directamente clases que heredan de JavaFX al iniciar.
 * </p>
 */
public class Launcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    /**
     * Inicia el ciclo de vida de JavaFX y entrega los argumentos recibidos.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) ->
                LOGGER.error("Uncaught exception on thread '{}'", thread.getName(), failure));
        LOGGER.info("Starting Gestor Enigma AES");
        Application.launch(MainApplication.class, args);
    }
}
