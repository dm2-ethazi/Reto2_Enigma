package com.enigma.javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class MainController {
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
        showNotImplemented("Seleccionar archivo");
    }

    @FXML
    private void chooseDestinationDirectory() {
        showNotImplemented("Seleccionar carpeta destino");
    }

    @FXML
    private void clearSelection() {
        showNotImplemented("Limpiar selección");
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
        showNotImplemented("Seleccionar archivo original");
    }

    private void showNotImplemented(String action) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Enigma");
        alert.setHeaderText(action);
        alert.setContentText("Esta función todavía no está implementada.");
        alert.showAndWait();
    }
}
