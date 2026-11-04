package com.enigma.javafx.controller;

import com.enigma.javafx.service.FileService;
import com.enigma.javafx.service.FileService.EncryptionResult;

import javafx.css.PseudoClass;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

/**
 * Controlador de la pantalla principal. Coordina la selección de archivos,
 * la ejecución asíncrona del cifrado y la entrega segura de la contraseña.
 *
 * <p>Los campos anotados con {@link FXML} se conectan con los elementos que
 * tienen el mismo {@code fx:id} en {@code main-view.fxml}. Los métodos de
 * interfaz anotados con {@link FXML} son invocados por los eventos definidos
 * en esa vista.</p>
 */
public class MainController {
    /** Estado visual aplicado al campo cuando se arrastra un archivo válido. */
    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");
    /** Operaciones de lectura y cifrado de ficheros. */
    private final FileService fileService = new FileService();
    /** Tarea activa de cifrado, si la hay. */
    private Task<EncryptionResult> encryptionTask;

    /** Campo de ruta del fichero que se cifrará. */
    @FXML
    private TextField sourcePathField;

    /** Campo de ruta de la carpeta de salida. */
    @FXML
    private TextField destinationDirectoryField;

    /** Ruta del original de referencia para la sección de descifrado aún no implementada. */
    @FXML
    private TextField referencePathField;

    /** Campo enmascarado de contraseña de descifrado; reservado para esa funcionalidad futura. */
    @FXML
    private PasswordField decryptPasswordField;

    /** Campo visible de contraseña de descifrado; reservado para esa funcionalidad futura. */
    @FXML
    private TextField visibleDecryptPasswordField;

    /** Campo de contraseña generada; PasswordField la oculta de forma predeterminada. */
    @FXML
    private PasswordField generatedPasswordField;

    /** Campo sincronizado que presenta la contraseña generada cuando el usuario la revela. */
    @FXML
    private TextField visibleGeneratedPasswordField;

    /** Alterna entre la representación enmascarada y la contraseña en texto visible. */
    @FXML
    private ToggleButton showGeneratedPasswordToggle;

    /** Botón que inicia el cifrado. */
    @FXML
    private Button encryptButton;

    /** Botón para seleccionar el archivo de entrada. */
    @FXML
    private Button chooseSourceButton;

    /** Botón para seleccionar la carpeta de destino. */
    @FXML
    private Button chooseDestinationButton;

    /** Botón para copiar la contraseña generada al portapapeles. */
    @FXML
    private Button copyPasswordButton;

    /** Botón para limpiar los campos de la pantalla. */
    @FXML
    private Button clearSelectionButton;

    /** Etiqueta donde se comunican el estado y los resultados de la operación. */
    @FXML
    private Label statusLabel;

    /**
     * Configura los controles después de que FXMLLoader los haya inyectado.
     *
     * <p>Los dos campos de contraseña comparten su texto, pero solo uno se
     * muestra a la vez. También se registran los eventos de arrastrar y soltar
     * para aceptar un único archivo normal como entrada.</p>
     */
    @FXML
    private void initialize() {
        generatedPasswordField.setEditable(false);
        visibleGeneratedPasswordField.setEditable(false);
        showGeneratedPasswordToggle.setDisable(true);
        visibleGeneratedPasswordField.textProperty()
                .bindBidirectional(generatedPasswordField.textProperty());
        visibleGeneratedPasswordField.visibleProperty()
                .bind(showGeneratedPasswordToggle.selectedProperty());
        visibleGeneratedPasswordField.managedProperty()
                .bind(showGeneratedPasswordToggle.selectedProperty());
        generatedPasswordField.visibleProperty()
                .bind(showGeneratedPasswordToggle.selectedProperty().not());
        generatedPasswordField.managedProperty()
                .bind(showGeneratedPasswordToggle.selectedProperty().not());
        showGeneratedPasswordToggle.textProperty().bind(
                javafx.beans.binding.Bindings.when(showGeneratedPasswordToggle.selectedProperty())
                        .then("Ocultar").otherwise("Mostrar"));
        showGeneratedPasswordToggle.accessibleTextProperty().bind(
                javafx.beans.binding.Bindings.when(showGeneratedPasswordToggle.selectedProperty())
                        .then("Ocultar contraseña generada")
                        .otherwise("Mostrar contraseña generada"));

        // El pseudoestado permite que la hoja CSS destaque una zona de soltado válida.
        sourcePathField.setOnDragOver(event -> {
            boolean accepted = hasSingleRegularFile(event.getDragboard());
            sourcePathField.pseudoClassStateChanged(DRAG_OVER, accepted);
            if (accepted) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // Al abandonar el campo se elimina el resaltado de arrastre.
        sourcePathField.setOnDragExited(event -> {
            sourcePathField.pseudoClassStateChanged(DRAG_OVER, false);
            event.consume();
        });

        // Solo se confirma la operación cuando se ha aceptado exactamente un archivo.
        sourcePathField.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = hasSingleRegularFile(dragboard);
            if (success) {
                setSourceFile(dragboard.getFiles().get(0));
            } else {
                statusLabel.setText("Arrastra un único archivo válido.");
            }
            sourcePathField.pseudoClassStateChanged(DRAG_OVER, false);
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /** Informa que la pantalla de ayuda todavía no forma parte de esta versión. */
    @FXML
    private void showHelp() {
        showNotImplemented("Ayuda");
    }

    /** Informa que la pantalla Acerca de todavía no está implementada. */
    @FXML
    private void showAbout() {
        showNotImplemented("Acerca de");
    }

    /** Abre el selector y carga el archivo que se cifrará. */
    @FXML
    private void chooseSourceFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivo de origen");
        setInitialDirectory(chooser, sourcePathField, destinationDirectoryField);

        File selectedFile = chooser.showOpenDialog(getOwnerWindow(sourcePathField));
        if (selectedFile != null) {
            setSourceFile(selectedFile);
        }
    }

    /** Abre el selector de carpeta para guardar el archivo cifrado. */
    @FXML
    private void chooseDestinationDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta de destino");
        setInitialDirectory(chooser, destinationDirectoryField, sourcePathField);

        File selectedDirectory = chooser.showDialog(getOwnerWindow(destinationDirectoryField));
        if (selectedDirectory != null) {
            destinationDirectoryField.setText(selectedDirectory.getAbsolutePath());
            statusLabel.setText("Carpeta de destino seleccionada.");
        }
    }

    /**
     * Limpia las rutas y credenciales visibles sin modificar ni borrar archivos.
     * La contraseña generada también queda oculta y el control Mostrar se desactiva.
     */
    @FXML
    private void clearSelection() {
        sourcePathField.clear();
        destinationDirectoryField.clear();
        referencePathField.clear();
        decryptPasswordField.clear();
        visibleDecryptPasswordField.clear();
        generatedPasswordField.clear();
        showGeneratedPasswordToggle.setSelected(false);
        showGeneratedPasswordToggle.setDisable(true);
        statusLabel.setText("Selección limpiada.");
    }

    /**
     * Valida la entrada y ejecuta el cifrado fuera del hilo gráfico de JavaFX.
     *
     * <p>Mientras la tarea trabaja, publica mensajes de progreso y deshabilita
     * los controles que podrían modificar sus rutas. Al finalizar, presenta la
     * contraseña solo en la vista enmascarada; ante un error la limpia.</p>
     */
    @FXML
    private void encryptFile() {
        // Impide que dos operaciones de cifrado se ejecuten simultáneamente.
        if (encryptionTask != null && encryptionTask.isRunning()) {
            return;
        }

        Path source;
        Path destination;
        try {
            source = selectedPath(sourcePathField);
            destination = selectedPath(destinationDirectoryField);
        } catch (InvalidPathException exception) {
            showError("Una de las rutas seleccionadas no es válida.");
            return;
        }

        if (source == null || destination == null) {
            showError("Selecciona un fichero de origen y una carpeta de destino.");
            return;
        }

        generatedPasswordField.clear();
        showGeneratedPasswordToggle.setSelected(false);
        showGeneratedPasswordToggle.setDisable(true);
        setEncryptionControlsDisabled(true);
        // Task.call se ejecuta en el hilo de trabajo, no en el hilo de la interfaz.
        encryptionTask = new Task<>() {
            @Override
            protected EncryptionResult call() throws IOException, GeneralSecurityException {
                long totalBytes = Files.size(source);
                updateMessage("Preparando cifrado...");
                EncryptionResult result = fileService.encrypt(source, destination, processed -> {
                    long percentage = totalBytes == 0 ? 100
                            : Math.min(100, Math.round(processed * 100.0 / totalBytes));
                    updateMessage("Cifrando archivo: " + percentage + "%");
                });
                updateMessage("Cifrado completado.");
                return result;
            }
        };

        // Los callbacks de estado de Task vuelven al hilo JavaFX de forma segura.
        statusLabel.textProperty().bind(encryptionTask.messageProperty());
        encryptionTask.setOnSucceeded(event -> {
            statusLabel.textProperty().unbind();
            setEncryptionControlsDisabled(false);
            EncryptionResult result = encryptionTask.getValue();
            generatedPasswordField.setText(result.password());
            showGeneratedPasswordToggle.setDisable(false);
            statusLabel.setText("Archivo cifrado: " + result.outputPath());
        });
        encryptionTask.setOnFailed(event -> {
            statusLabel.textProperty().unbind();
            setEncryptionControlsDisabled(false);
            generatedPasswordField.clear();
            Throwable failure = encryptionTask.getException();
            showError(failure.getMessage() == null
                    ? "No se pudo cifrar el archivo." : failure.getMessage());
        });
        encryptionTask.setOnCancelled(event -> {
            statusLabel.textProperty().unbind();
            setEncryptionControlsDisabled(false);
            generatedPasswordField.clear();
            statusLabel.setText("Cifrado cancelado.");
        });

        // Un hilo daemon permite cerrar la aplicación sin dejar un proceso colgado.
        Thread worker = new Thread(encryptionTask, "enigma-aes-encryption");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Copia al portapapeles la contraseña del último cifrado completado.
     * No permite copiar un campo vacío o una operación que no haya terminado.
     */
    @FXML
    private void copyGeneratedPassword() {
        String password = generatedPasswordField.getText();
        if (password == null || password.isBlank()) {
            statusLabel.setText("Cifra un archivo antes de copiar su contraseña.");
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(password);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Contraseña copiada al portapapeles.");
    }

    /** Mantiene el evento de la vista, pero no realiza descifrado en esta versión. */
    @FXML
    private void decryptFile() {
        showNotImplemented("Descifrar archivo");
    }

    /** Abre el selector del archivo de referencia usado por una función futura. */
    @FXML
    private void chooseReferenceFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivo original");
        setInitialDirectory(chooser, referencePathField, sourcePathField);

        File selectedFile = chooser.showOpenDialog(getOwnerWindow(referencePathField));
        if (selectedFile != null) {
            referencePathField.setText(selectedFile.getAbsolutePath());
            statusLabel.setText("Archivo original seleccionado.");
        }
    }

    /** Actualiza la ruta de origen después de elegir o arrastrar un archivo. */
    private void setSourceFile(File file) {
        sourcePathField.setText(file.getAbsolutePath());
        statusLabel.setText("Archivo de entrada seleccionado.");
    }

    /**
     * Comprueba que el gesto de arrastre contenga un único archivo normal.
     *
     * @param dragboard contenido que el sistema operativo entrega al arrastrar
     * @return {@code true} si el contenido puede usarse como archivo de origen
     */
    private boolean hasSingleRegularFile(Dragboard dragboard) {
        return dragboard.hasFiles()
                && dragboard.getFiles().size() == 1
                && dragboard.getFiles().get(0).isFile();
    }

    /**
     * Ajusta la carpeta inicial del selector de archivos a partir de las rutas actuales.
     *
     * @param chooser selector de archivo que se va a mostrar
     * @param pathFields campos de ruta en los que buscar una carpeta existente
     */
    private void setInitialDirectory(FileChooser chooser, TextField... pathFields) {
        File directory = findInitialDirectory(pathFields);
        if (directory != null) {
            chooser.setInitialDirectory(directory);
        }
    }

    /**
     * Ajusta la carpeta inicial del selector de carpetas a partir de las rutas actuales.
     *
     * @param chooser selector de carpetas que se va a mostrar
     * @param pathFields campos de ruta en los que buscar una carpeta existente
     */
    private void setInitialDirectory(DirectoryChooser chooser, TextField... pathFields) {
        File directory = findInitialDirectory(pathFields);
        if (directory != null) {
            chooser.setInitialDirectory(directory);
        }
    }

    /**
     * Devuelve la primera carpeta existente asociada a las rutas proporcionadas.
     * Si un campo apunta a un archivo, se considera su carpeta contenedora.
     *
     * @param pathFields campos que pueden contener rutas previas
     * @return carpeta inicial apropiada o {@code null} si ninguna está disponible
     */
    private File findInitialDirectory(TextField... pathFields) {
        for (TextField pathField : pathFields) {
            String path = pathField.getText();
            if (path == null || path.isBlank()) {
                continue;
            }

            File selected = new File(path);
            File directory = selected.isDirectory() ? selected : selected.getParentFile();
            if (directory != null && directory.isDirectory()) {
                return directory;
            }
        }
        return null;
    }

    /**
     * Obtiene la ventana propietaria de un nodo para mostrar diálogos modales.
     *
     * @param node control perteneciente a la ventana
     * @return ventana asociada, o {@code null} si el nodo aún no está en una escena
     */
    private Window getOwnerWindow(Node node) {
        return node.getScene() == null ? null : node.getScene().getWindow();
    }

    /**
     * Convierte el texto de un control en una ruta del sistema de archivos.
     *
     * @param field campo que contiene la ruta
     * @return ruta introducida o {@code null} si el campo está vacío
     * @throws InvalidPathException si el texto no representa una ruta válida
     */
    private static Path selectedPath(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : Path.of(value);
    }

    /**
     * Bloquea o habilita los controles que no deben usarse mientras la tarea trabaja.
     *
     * @param disabled {@code true} para bloquearlas; {@code false} para habilitarlas
     */
    private void setEncryptionControlsDisabled(boolean disabled) {
        encryptButton.setDisable(disabled);
        chooseSourceButton.setDisable(disabled);
        chooseDestinationButton.setDisable(disabled);
        copyPasswordButton.setDisable(disabled);
        clearSelectionButton.setDisable(disabled);
    }

    /**
     * Presenta un mensaje de error y lo asocia a la ventana principal si está disponible.
     *
     * @param message explicación del problema para la persona usuaria
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de cifrado");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Window owner = getOwnerWindow(sourcePathField);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    /**
     * Presenta un aviso informativo para una acción que aún no está disponible.
     *
     * @param action nombre de la acción seleccionada
     */
    private void showNotImplemented(String action) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Enigma");
        alert.setHeaderText(action);
        alert.setContentText("Esta función todavía no está implementada.");
        alert.showAndWait();
    }
}
