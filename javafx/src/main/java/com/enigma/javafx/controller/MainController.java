package com.enigma.javafx.controller;

import javafx.fxml.FXML;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

public class MainController {
    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");

    @FXML
    private TextField sourcePathField;

    @FXML
    private TextField destinationDirectoryField;

    @FXML
    private TextField referencePathField;

    @FXML
    private PasswordField decryptPasswordField;

    @FXML
    private TextField visibleDecryptPasswordField;

    @FXML
    private TextField generatedPasswordField;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        sourcePathField.setOnDragOver(event -> {
            boolean accepted = hasSingleRegularFile(event.getDragboard());
            sourcePathField.pseudoClassStateChanged(DRAG_OVER, accepted);
            if (accepted) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        sourcePathField.setOnDragExited(event -> {
            sourcePathField.pseudoClassStateChanged(DRAG_OVER, false);
            event.consume();
        });

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

    @FXML
    private void showHelp() {
        showNotImplemented("Ayuda");
    }

    @FXML
    private void showAbout() {
        showNotImplemented("Acerca de");
    }

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

    @FXML
    private void clearSelection() {
        sourcePathField.clear();
        destinationDirectoryField.clear();
        referencePathField.clear();
        decryptPasswordField.clear();
        visibleDecryptPasswordField.clear();
        generatedPasswordField.clear();
        statusLabel.setText("Selección limpiada.");
    }

    @FXML
    private void encryptFile() {
        showNotImplemented("Cifrar archivo");
    }

    @FXML
    private void copyGeneratedPassword() {
        showNotImplemented("Copiar contraseña");
    }

    @FXML
    private void decryptFile() {
        showNotImplemented("Descifrar archivo");
    }

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

    private void setSourceFile(File file) {
        sourcePathField.setText(file.getAbsolutePath());
        statusLabel.setText("Archivo de entrada seleccionado.");
    }

    private boolean hasSingleRegularFile(Dragboard dragboard) {
        return dragboard.hasFiles()
                && dragboard.getFiles().size() == 1
                && dragboard.getFiles().get(0).isFile();
    }

    private void setInitialDirectory(FileChooser chooser, TextField... pathFields) {
        File directory = findInitialDirectory(pathFields);
        if (directory != null) {
            chooser.setInitialDirectory(directory);
        }
    }

    private void setInitialDirectory(DirectoryChooser chooser, TextField... pathFields) {
        File directory = findInitialDirectory(pathFields);
        if (directory != null) {
            chooser.setInitialDirectory(directory);
        }
    }

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

    private Window getOwnerWindow(Node node) {
        return node.getScene() == null ? null : node.getScene().getWindow();
    }

    private void showNotImplemented(String action) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Enigma");
        alert.setHeaderText(action);
        alert.setContentText("Esta función todavía no está implementada.");
        alert.showAndWait();
    }
}
