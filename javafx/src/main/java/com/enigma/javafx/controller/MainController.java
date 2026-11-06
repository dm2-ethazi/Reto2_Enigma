package com.enigma.javafx.controller;

import com.enigma.javafx.service.FileService;
import com.enigma.javafx.service.FileService.DecryptionResult;
import com.enigma.javafx.service.FileService.EncryptionResult;
import com.enigma.javafx.i18n.LanguageService;
import com.enigma.javafx.i18n.LanguageService.Language;

import javafx.css.PseudoClass;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;

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
    /** Tarea activa de descifrado, si la hay. */
    private Task<DecryptionResult> decryptionTask;
    private volatile Language selectedLanguage = Language.SPANISH;
    private volatile LocalizedText currentStatus =
            new LocalizedText("Listo para trabajar.", new Object[0]);

    @FXML
    private Parent rootPane;

    @FXML
    private Label languageLabel;

    @FXML
    private ComboBox<Language> languageSelector;

    /** Campo de ruta del fichero que se cifrará. */
    @FXML
    private TextField sourcePathField;

    /** Campo de ruta de la carpeta de salida. */
    @FXML
    private TextField destinationDirectoryField;

    /** Ruta del contenedor .enc que se va a descifrar, independiente del origen de cifrado. */
    @FXML
    private TextField decryptSourcePathField;

    /** Ruta opcional del original usado para verificar un archivo descifrado. */
    @FXML
    private TextField referencePathField;

    /** Campo enmascarado de la contraseña del contenedor que se va a descifrar. */
    @FXML
    private PasswordField decryptPasswordField;

    /** Campo sincronizado que muestra la contraseña de descifrado cuando se revela. */
    @FXML
    private TextField visibleDecryptPasswordField;

    /** Alterna la visibilidad del campo de contraseña de descifrado. */
    @FXML
    private ToggleButton showPasswordToggle;

    /** Opción para comprobar el resultado frente al archivo original. */
    @FXML
    private CheckBox verifyCheckbox;

    /** Icono que indica que la contraseña de descifrado está oculta. */
    @FXML
    private FontIcon showDecryptPasswordIcon;

    /** Icono que indica que la contraseña de descifrado está visible. */
    @FXML
    private FontIcon hideDecryptPasswordIcon;

    /** Campo de contraseña generada; PasswordField la oculta de forma predeterminada. */
    @FXML
    private PasswordField generatedPasswordField;

    /** Campo sincronizado que presenta la contraseña generada cuando el usuario la revela. */
    @FXML
    private TextField visibleGeneratedPasswordField;

    /** Alterna entre la representación enmascarada y la contraseña en texto visible. */
    @FXML
    private ToggleButton showGeneratedPasswordToggle;

    /** Icono para indicar que la contraseña generada está oculta. */
    @FXML
    private FontIcon showGeneratedPasswordIcon;

    /** Icono para indicar que la contraseña generada está visible. */
    @FXML
    private FontIcon hideGeneratedPasswordIcon;

    /** Botón que inicia el cifrado. */
    @FXML
    private Button encryptButton;

    /** Botón para seleccionar el archivo de entrada. */
    @FXML
    private Button chooseSourceButton;

    /** Botón para seleccionar la carpeta de destino. */
    @FXML
    private Button chooseDestinationButton;

    /** Botón que inicia el descifrado. */
    @FXML
    private Button decryptButton;

    /** Botón para seleccionar el contenedor que se va a descifrar. */
    @FXML
    private Button chooseEncryptedFileButton;

    /** Botón para seleccionar el archivo original usado en la verificación opcional. */
    @FXML
    private Button chooseReferenceButton;

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
        configurePasswordVisibility(
                showPasswordToggle, showDecryptPasswordIcon, hideDecryptPasswordIcon,
                decryptPasswordField, visibleDecryptPasswordField, "contraseña");
        configurePasswordVisibility(
                showGeneratedPasswordToggle, showGeneratedPasswordIcon, hideGeneratedPasswordIcon,
                generatedPasswordField, visibleGeneratedPasswordField,
                "contraseña generada");
        generatedPasswordField.setEditable(false);
        visibleGeneratedPasswordField.setEditable(false);
        showGeneratedPasswordToggle.setDisable(true);
        languageSelector.getItems().setAll(Language.values());
        languageSelector.setValue(Language.SPANISH);
        languageSelector.setOnAction(event -> changeLanguage());
        applyLanguage(Language.SPANISH);

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
                setStatus("Arrastra un único archivo válido.");
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
        chooser.setTitle(translate("Seleccionar archivo de origen"));
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
        chooser.setTitle(translate("Seleccionar carpeta de destino"));
        setInitialDirectory(chooser, destinationDirectoryField, sourcePathField);

        File selectedDirectory = chooser.showDialog(getOwnerWindow(destinationDirectoryField));
        if (selectedDirectory != null) {
            destinationDirectoryField.setText(selectedDirectory.getAbsolutePath());
            setStatus("Carpeta de destino seleccionada.");
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
        decryptSourcePathField.clear();
        referencePathField.clear();
        decryptPasswordField.clear();
        visibleDecryptPasswordField.clear();
        generatedPasswordField.clear();
        showGeneratedPasswordToggle.setSelected(false);
        showGeneratedPasswordToggle.setDisable(true);
        setStatus("Selección limpiada.");
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
        if (isOperationRunning()) {
            return;
        }

        Path source;
        Path destination;
        try {
            source = selectedPath(sourcePathField);
            destination = selectedPath(destinationDirectoryField);
        } catch (InvalidPathException exception) {
            showError(translate("Una de las rutas seleccionadas no es válida."));
            return;
        }

        if (source == null || destination == null) {
            showError(translate("Selecciona un fichero de origen y una carpeta de destino."));
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
                updateMessage(setTaskStatus("Preparando cifrado..."));
                EncryptionResult result = fileService.encrypt(source, destination, processed -> {
                    long percentage = totalBytes == 0 ? 100
                            : Math.min(100, Math.round(processed * 100.0 / totalBytes));
                    updateMessage(setTaskStatus("Cifrando archivo: {0}%", percentage));
                });
                updateMessage(setTaskStatus("Cifrado completado."));
                return result;
            }
        };

        // Los callbacks de estado de Task vuelven al hilo JavaFX de forma segura.
        observeTaskMessages(encryptionTask);
        encryptionTask.setOnSucceeded(event -> {
            setEncryptionControlsDisabled(false);
            EncryptionResult result = encryptionTask.getValue();
            generatedPasswordField.setText(result.password());
            showGeneratedPasswordToggle.setDisable(false);
            setStatus("Archivo cifrado: {0}", result.outputPath());
        });
        encryptionTask.setOnFailed(event -> {
            setEncryptionControlsDisabled(false);
            generatedPasswordField.clear();
            Throwable failure = encryptionTask.getException();
            showError(translateFailure(failure, "No se pudo cifrar el archivo."));
        });
        encryptionTask.setOnCancelled(event -> {
            setEncryptionControlsDisabled(false);
            generatedPasswordField.clear();
            setStatus("Cifrado cancelado.");
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
            setStatus("Cifra un archivo antes de copiar su contraseña.");
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(password);
        Clipboard.getSystemClipboard().setContent(content);
        setStatus("Contraseña copiada al portapapeles.");
    }

    /** Descifra el contenedor seleccionado sin modificar la vista previa. */
    @FXML
    private void decryptFile() {
        if (isOperationRunning()) {
            return;
        }

        Path encryptedFile;
        Path destination;
        Path selectedReference = null;
        try {
            encryptedFile = selectedPath(decryptSourcePathField);
            destination = selectedPath(destinationDirectoryField);
            if (verifyCheckbox.isSelected()) {
                selectedReference = selectedPath(referencePathField);
            }
        } catch (InvalidPathException exception) {
            showError(translate("Una de las rutas seleccionadas no es válida."));
            return;
        }

        if (encryptedFile == null || destination == null) {
            showError(translate("Selecciona el contenedor .enc y una carpeta de destino."));
            return;
        }
        if (decryptPasswordField.getText() == null || decryptPasswordField.getText().isBlank()) {
            showError(translate("Introduce la contraseña del contenedor."));
            return;
        }
        if (verifyCheckbox.isSelected() && selectedReference == null) {
            showError(translate("Selecciona el archivo original para poder compararlo."));
            return;
        }

        Path reference = selectedReference;
        char[] password = decryptPasswordField.getText().toCharArray();
        setEncryptionControlsDisabled(true);
        decryptionTask = new Task<>() {
            @Override
            protected DecryptionResult call() throws IOException, GeneralSecurityException {
                try {
                    long totalBytes = Files.size(encryptedFile);
                    updateMessage(setTaskStatus("Preparando descifrado..."));
                    DecryptionResult result = fileService.decrypt(
                            encryptedFile, destination, password, reference, processed -> {
                                long percentage = totalBytes == 0 ? 100
                                        : Math.min(100, Math.round(processed * 100.0 / totalBytes));
                                updateMessage(setTaskStatus("Descifrando archivo: {0}%", percentage));
                            });
                    updateMessage(setTaskStatus("Descifrado completado."));
                    return result;
                } finally {
                    Arrays.fill(password, '\0');
                }
            }
        };

        observeTaskMessages(decryptionTask);
        decryptionTask.setOnSucceeded(event -> {
            setEncryptionControlsDisabled(false);
            setStatus("Archivo descifrado: {0}", decryptionTask.getValue().outputPath());
        });
        decryptionTask.setOnFailed(event -> {
            setEncryptionControlsDisabled(false);
            Throwable failure = decryptionTask.getException();
            showError(translateFailure(failure, "No se pudo descifrar el archivo."));
        });
        decryptionTask.setOnCancelled(event -> {
            setEncryptionControlsDisabled(false);
            setStatus("Descifrado cancelado.");
        });

        Thread worker = new Thread(decryptionTask, "enigma-aes-decryption");
        worker.setDaemon(true);
        worker.start();
    }

    /** Abre el selector para elegir el contenedor .enc que se va a descifrar. */
    @FXML
    private void chooseEncryptedFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(translate("Seleccionar archivo cifrado"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        translate("Contenedores Enigma (*.enc)"), "*.enc"));
        setInitialDirectory(chooser, decryptSourcePathField, sourcePathField);

        File selectedFile = chooser.showOpenDialog(getOwnerWindow(decryptSourcePathField));
        if (selectedFile != null) {
            decryptSourcePathField.setText(selectedFile.getAbsolutePath());
            setStatus("Archivo cifrado seleccionado.");
        }
    }

    /** Abre el selector del archivo original para la verificación opcional. */
    @FXML
    private void chooseReferenceFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(translate("Seleccionar archivo original"));
        setInitialDirectory(chooser, referencePathField, sourcePathField);

        File selectedFile = chooser.showOpenDialog(getOwnerWindow(referencePathField));
        if (selectedFile != null) {
            referencePathField.setText(selectedFile.getAbsolutePath());
            setStatus("Archivo original seleccionado.");
        }
    }

    /** Actualiza la ruta de origen después de elegir o arrastrar un archivo. */
    private void setSourceFile(File file) {
        sourcePathField.setText(file.getAbsolutePath());
        setStatus("Archivo de entrada seleccionado.");
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
        decryptButton.setDisable(disabled);
        chooseEncryptedFileButton.setDisable(disabled);
        chooseSourceButton.setDisable(disabled);
        chooseDestinationButton.setDisable(disabled);
        chooseReferenceButton.setDisable(disabled);
        decryptPasswordField.setDisable(disabled);
        visibleDecryptPasswordField.setDisable(disabled);
        showPasswordToggle.setDisable(disabled);
        verifyCheckbox.setDisable(disabled);
        copyPasswordButton.setDisable(disabled);
        clearSelectionButton.setDisable(disabled);
    }

    /** Informa si alguna de las operaciones criptográficas sigue ejecutándose. */
    private boolean isOperationRunning() {
        return encryptionTask != null && encryptionTask.isRunning()
                || decryptionTask != null && decryptionTask.isRunning();
    }

    /**
     * Presenta un mensaje de error y lo asocia a la ventana principal si está disponible.
     *
     * @param message explicación del problema para la persona usuaria
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        styleAlert(alert, "app-alert-error");
        alert.setTitle(translate("Error de cifrado"));
        alert.setHeaderText(null);
        alert.setContentText(translate(message));
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
        styleAlert(alert, "app-alert-info");
        alert.setTitle("Enigma");
        alert.setHeaderText(translate(action));
        alert.setContentText(translate("Esta función todavía no está implementada."));
        alert.showAndWait();
    }

    private void changeLanguage() {
        Language language = languageSelector.getValue();
        if (language != null) {
            applyLanguage(language);
        }
    }

    private void applyLanguage(Language language) {
        selectedLanguage = language;
        translateNode(rootPane);
        statusLabel.setText(renderStatus(currentStatus));
        updatePasswordVisibilityText(showPasswordToggle, "contraseña");
        updatePasswordVisibilityText(showGeneratedPasswordToggle, "contraseña generada");
    }

    private void translateNode(Node node) {
        if (node instanceof Labeled labeled) {
            labeled.setText(translate(labeled.getText()));
        }
        if (node instanceof TextInputControl input) {
            input.setPromptText(translate(input.getPromptText()));
        }
        node.setAccessibleText(translate(node.getAccessibleText()));

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                translateNode(child);
            }
        }
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            translateNode(scrollPane.getContent());
        }
    }

    private String translate(String text) {
        return LanguageService.translate(selectedLanguage, text);
    }

    private void setStatus(String spanishTemplate, Object... arguments) {
        currentStatus = new LocalizedText(spanishTemplate, arguments);
        statusLabel.setText(renderStatus(currentStatus));
    }

    private String setTaskStatus(String spanishTemplate, Object... arguments) {
        currentStatus = new LocalizedText(spanishTemplate, arguments);
        return renderStatus(currentStatus);
    }

    private String renderStatus(LocalizedText status) {
        return LanguageService.format(selectedLanguage, status.template(), status.arguments());
    }

    private void observeTaskMessages(Task<?> task) {
        task.messageProperty().addListener((observable, oldMessage, newMessage) -> {
            if (newMessage != null && !newMessage.isBlank()) {
                statusLabel.setText(renderStatus(currentStatus));
            }
        });
    }

    private void updatePasswordVisibilityText(ToggleButton toggle, String passwordName) {
        String verb = toggle.isSelected() ? "Ocultar " : "Mostrar ";
        toggle.setAccessibleText(translate(verb + passwordName));
    }

    private String translateFailure(Throwable failure, String fallback) {
        if (failure instanceof FileAlreadyExistsException alreadyExists) {
            return LanguageService.format(selectedLanguage,
                    "El archivo de destino ya existe; no se sobrescribirá: {0}",
                    alreadyExists.getFile());
        }
        if (failure instanceof FileSystemException) {
            return translate(
                    "No se pudo completar la operación de archivo. Comprueba los permisos y el espacio disponible.");
        }
        if (failure instanceof javax.crypto.AEADBadTagException) {
            return translate("La contraseña es incorrecta o el archivo cifrado está dañado.");
        }
        if (failure == null || failure.getMessage() == null) {
            return translate(fallback);
        }
        String translated = translate(failure.getMessage());
        return translated.equals(failure.getMessage())
                && (failure instanceof IOException || failure instanceof GeneralSecurityException)
                ? translate(fallback) : translated;
    }

    private record LocalizedText(String template, Object[] arguments) {
    }

    /**
     * Aplica a los diálogos la misma paleta que usa la ventana principal.
     *
     * @param alert diálogo que se va a mostrar
     * @param styleClass clase CSS que identifica el tipo de aviso
     */
    private void styleAlert(Alert alert, String styleClass) {
        var stylesheet = MainController.class.getResource("/com/enigma/javafx/styles.css");
        if (stylesheet == null) {
            throw new IllegalStateException("No se encontró la hoja de estilos de la aplicación.");
        }
        alert.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());
        alert.getDialogPane().getStyleClass().addAll("app-alert", styleClass);
    }

    /**
     * Sincroniza los campos de contraseña y alterna el texto y los iconos del botón.
     * El ojo indica que se puede mostrar el valor; el ojo tachado, que se puede ocultar.
     */
    private void configurePasswordVisibility(ToggleButton toggle, FontIcon showIcon,
                                            FontIcon hideIcon, PasswordField maskedField,
                                            TextField visibleField, String accessibleName) {
        Color magenta = Color.web("#D63378");
        showIcon.setIconColor(magenta);
        hideIcon.setIconColor(magenta);
        visibleField.textProperty().bindBidirectional(maskedField.textProperty());
        visibleField.visibleProperty().bind(toggle.selectedProperty());
        visibleField.managedProperty().bind(toggle.selectedProperty());
        maskedField.visibleProperty().bind(toggle.selectedProperty().not());
        maskedField.managedProperty().bind(toggle.selectedProperty().not());
        showIcon.visibleProperty().bind(toggle.selectedProperty().not());
        showIcon.managedProperty().bind(toggle.selectedProperty().not());
        hideIcon.visibleProperty().bind(toggle.selectedProperty());
        hideIcon.managedProperty().bind(toggle.selectedProperty());
        toggle.selectedProperty().addListener(
                (observable, wasSelected, isSelected) ->
                        updatePasswordVisibilityText(toggle, accessibleName));
        updatePasswordVisibilityText(toggle, accessibleName);
    }
}
