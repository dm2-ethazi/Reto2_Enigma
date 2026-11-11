package com.enigma.javafx;

import javafx.application.Application;

/**
 * Punto de entrada estable de la aplicación.
 *
 * <p>
 * Se separa de {@link MainApplication} para que el lanzador de Java no
 * tenga que resolver directamente clases que heredan de JavaFX al iniciar.
 * </p>
 */
public class Launcher {
    /**
     * Inicia el ciclo de vida de JavaFX y entrega los argumentos recibidos.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        Application.launch(MainApplication.class, args);
    }
}
