package com.enigma.javafx.controller;

import com.enigma.javafx.MainApplication;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainControllerGeneralTest extends JavaFxTestSupport {
    @Test
    void initializesPasswordVisibilityAndClearsAllInputFields() throws Exception {
        MainController controller = loadController();

        PasswordField maskedPassword = field(controller, "generatedPasswordField");
        TextField visiblePassword = field(controller, "visibleGeneratedPasswordField");
        ToggleButton visibilityToggle = field(controller, "showGeneratedPasswordToggle");
        ToggleButton decryptVisibilityToggle = field(controller, "showPasswordToggle");
        assertTrue(maskedPassword.isVisible());
        assertFalse(visiblePassword.isVisible());
        assertTrue(visibilityToggle.isDisable());
        var showGeneratedIcon = field(controller, "showGeneratedPasswordIcon",
                org.kordamp.ikonli.javafx.FontIcon.class);
        var hideGeneratedIcon = field(controller, "hideGeneratedPasswordIcon",
                org.kordamp.ikonli.javafx.FontIcon.class);
        var showDecryptIcon = field(controller, "showDecryptPasswordIcon",
                org.kordamp.ikonli.javafx.FontIcon.class);
        var hideDecryptIcon = field(controller, "hideDecryptPasswordIcon",
                org.kordamp.ikonli.javafx.FontIcon.class);
        assertTrue(showGeneratedIcon.isVisible());
        assertFalse(hideGeneratedIcon.isVisible());
        assertTrue(showDecryptIcon.isVisible());
        assertFalse(hideDecryptIcon.isVisible());
        assertEquals("fas-eye", showGeneratedIcon.getIconLiteral());
        assertEquals("fas-eye-slash", hideGeneratedIcon.getIconLiteral());
        assertEquals("fas-eye", showDecryptIcon.getIconLiteral());
        assertEquals("fas-eye-slash", hideDecryptIcon.getIconLiteral());
        assertEquals(javafx.scene.paint.Color.web("#D63378"), showGeneratedIcon.getIconColor());
        assertEquals(javafx.scene.paint.Color.web("#D63378"), hideGeneratedIcon.getIconColor());
        assertEquals(javafx.scene.paint.Color.web("#D63378"), showDecryptIcon.getIconColor());
        assertEquals(javafx.scene.paint.Color.web("#D63378"), hideDecryptIcon.getIconColor());

        runOnFxThread(() -> {
            visibilityToggle.setSelected(true);
            decryptVisibilityToggle.setSelected(true);
            maskedPassword.setText("SafePassword1234");
            field(controller, "sourcePathField", TextField.class).setText("source.txt");
            field(controller, "destinationDirectoryField", TextField.class).setText("output");
            field(controller, "decryptSourcePathField", TextField.class).setText("encrypted.enc");
            field(controller, "referencePathField", TextField.class).setText("reference.txt");
            field(controller, "decryptPasswordField", PasswordField.class).setText("decrypt");
            field(controller, "visibleDecryptPasswordField", TextField.class).setText("decrypt");
            invoke(controller, "clearSelection");
            return null;
        });

        assertEquals("", maskedPassword.getText());
        assertEquals("", visiblePassword.getText());
        assertFalse(visibilityToggle.isSelected());
        assertEquals("", visibilityToggle.getText());
        assertEquals("", decryptVisibilityToggle.getText());
        assertFalse(showDecryptIcon.isVisible());
        assertTrue(hideDecryptIcon.isVisible());
        assertEquals("", field(controller, "decryptSourcePathField", TextField.class).getText());
        assertTrue(visibilityToggle.isDisable());
        assertEquals("Selección limpiada.", field(controller, "statusLabel", Label.class).getText());
    }

    @Test
    void switchesInterfaceLanguageWithoutChangingSelectedPathsOrFileContents() throws Exception {
        MainController controller = loadController();
        Path file = Files.createTempFile("enigma-language", ".txt");
        byte[] originalContents = "Contenido original: ñ, € y 漢字".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, originalContents);

        runOnFxThread(() -> {
            ComboBox<?> languageSelector = field(controller, "languageSelector");
            TextField sourcePath = field(controller, "sourcePathField", TextField.class);
            TextField destinationPath = field(controller, "destinationDirectoryField", TextField.class);
            PasswordField password = field(controller, "decryptPasswordField", PasswordField.class);
            Button encryptButton = field(controller, "encryptButton", Button.class);
            Label status = field(controller, "statusLabel", Label.class);
            ToggleButton passwordToggle = field(controller, "showPasswordToggle", ToggleButton.class);
            ToggleButton generatedPasswordToggle =
                    field(controller, "showGeneratedPasswordToggle", ToggleButton.class);

            sourcePath.setText(file.toString());
            destinationPath.setText(file.getParent().toString());
            password.setText("KeepThisPassword123");

            languageSelector.getSelectionModel().select(1);
            languageSelector.fireEvent(new ActionEvent());
            assertEquals("Encrypt file", encryptButton.getText());
            assertEquals("Language",
                    field(controller, "languageLabel", Label.class).getText());
            assertEquals("Ready.", status.getText());
            assertEquals("Drop a file here or choose one", sourcePath.getPromptText());
            assertEquals("Show password", passwordToggle.getAccessibleText());
            assertEquals("Show generated password", generatedPasswordToggle.getAccessibleText());
            passwordToggle.setSelected(true);
            assertEquals("Hide password", passwordToggle.getAccessibleText());
            passwordToggle.setSelected(false);
            assertEquals("Show password", passwordToggle.getAccessibleText());

            languageSelector.getSelectionModel().select(2);
            languageSelector.fireEvent(new ActionEvent());
            assertEquals("Zifratu fitxategia", encryptButton.getText());
            assertEquals("Prest.", status.getText());
            assertEquals("Arrastatu fitxategi bat hona edo aukeratu", sourcePath.getPromptText());

            languageSelector.getSelectionModel().select(0);
            languageSelector.fireEvent(new ActionEvent());
            assertEquals("Cifrar archivo", encryptButton.getText());
            assertEquals("Listo para trabajar.", status.getText());
            assertEquals(file.toString(), sourcePath.getText());
            assertEquals(file.getParent().toString(), destinationPath.getText());
            assertEquals("KeepThisPassword123", password.getText());
            return null;
        });

        assertArrayEquals(originalContents, Files.readAllBytes(file));
    }

    @Test
    void translatesAnActiveOperationStatusWhenLanguageChanges() throws Exception {
        MainController controller = loadController();

        runOnFxThread(() -> {
            Label status = field(controller, "statusLabel", Label.class);
            ComboBox<?> languageSelector = field(controller, "languageSelector");

            String progressMessage = (String) invoke(controller, "setTaskStatus",
                    new Class<?>[] {String.class, Object[].class},
                    "Cifrando archivo: {0}%", new Object[] {42});
            status.setText(progressMessage);
            assertEquals("Cifrando archivo: 42%", status.getText());

            languageSelector.getSelectionModel().select(1);
            languageSelector.fireEvent(new ActionEvent());
            assertEquals("Encrypting file: 42%", status.getText());

            languageSelector.getSelectionModel().select(2);
            languageSelector.fireEvent(new ActionEvent());
            assertEquals("Fitxategia zifratzen: 42%", status.getText());

            languageSelector.getSelectionModel().select(0);
            languageSelector.fireEvent(new ActionEvent());
            assertEquals("Cifrando archivo: 42%", status.getText());
            return null;
        });
    }

    @Test
    void selectsSourceFileAndFindsExistingDirectoriesFromPathFields() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-controller");
        Path source = Files.createFile(directory.resolve("source.txt"));

        runOnFxThread(() -> {
            invoke(controller, "setSourceFile", new Class<?>[] {File.class}, source.toFile());
            return null;
        });

        assertEquals(source.toAbsolutePath().toString(),
                field(controller, "sourcePathField", TextField.class).getText());
        assertEquals("Archivo de entrada seleccionado.",
                field(controller, "statusLabel", Label.class).getText());

        File selectedDirectory = runOnFxThread(() -> (File) invoke(controller,
                "findInitialDirectory", new Class<?>[] {TextField[].class},
                (Object) new TextField[] {field(controller, "sourcePathField")}));
        assertEquals(directory.toFile(), selectedDirectory);

        File noDirectory = runOnFxThread(() -> (File) invoke(controller,
                "findInitialDirectory", new Class<?>[] {TextField[].class},
                (Object) new TextField[] {new TextField()}));
        assertNull(noDirectory);

        File selectedFolder = runOnFxThread(() -> {
            TextField folderField = new TextField(directory.toString());
            return (File) invoke(controller, "findInitialDirectory",
                    new Class<?>[] {TextField[].class}, (Object) new TextField[] {folderField});
        });
        assertEquals(directory.toFile(), selectedFolder);
    }

    @Test
    void convertsOptionalPathsAndReportsMissingOwnerWindow() throws Exception {
        MainController controller = loadController();
        TextField pathField = field(controller, "sourcePathField");

        Path emptyPath = runOnFxThread(() -> (Path) invoke(controller,
                "selectedPath", new Class<?>[] {TextField.class}, pathField));
        assertNull(emptyPath);

        Path expected = Path.of("source.txt");
        runOnFxThread(() -> {
            pathField.setText(expected.toString());
            return null;
        });
        Path actual = runOnFxThread(() -> (Path) invoke(controller,
                "selectedPath", new Class<?>[] {TextField.class}, pathField));
        assertEquals(expected, actual);

        Window owner = runOnFxThread(() -> (Window) invoke(controller,
                "getOwnerWindow", new Class<?>[] {javafx.scene.Node.class}, pathField));
        assertNull(owner);

        Stage stage = runOnFxThread(() -> {
            Parent root = pathField;
            while (root.getParent() != null) {
                root = root.getParent();
            }
            Stage ownerStage = new Stage();
            ownerStage.setScene(new Scene(root));
            return ownerStage;
        });
        assertEquals(stage, runOnFxThread(() -> (Window) invoke(controller,
                "getOwnerWindow", new Class<?>[] {javafx.scene.Node.class}, pathField)));
        runOnFxThread(() -> {
            stage.close();
            return null;
        });
    }

    @Test
    void enablesAndDisablesEncryptionControls() throws Exception {
        MainController controller = loadController();

        runOnFxThread(() -> {
            invoke(controller, "setEncryptionControlsDisabled",
                    new Class<?>[] {boolean.class}, true);
            return null;
        });
        assertControlsDisabled(controller, true);

        runOnFxThread(() -> {
            invoke(controller, "setEncryptionControlsDisabled",
                    new Class<?>[] {boolean.class}, false);
            return null;
        });
        assertControlsDisabled(controller, false);
    }

    @Test
    void clearingPasswordCopyAttemptExplainsThatEncryptionIsNeeded() throws Exception {
        MainController controller = loadController();

        runOnFxThread(() -> {
            invoke(controller, "copyGeneratedPassword");
            return null;
        });

        assertEquals("Cifra un archivo antes de copiar su contraseña.",
                field(controller, "statusLabel", Label.class).getText());
    }

    @Test
    void copiesGeneratedPasswordToSystemClipboard() throws Exception {
        MainController controller = loadController();
        runOnFxThread(() -> {
            field(controller, "generatedPasswordField", PasswordField.class)
                    .setText("SafePassword1234");
            invoke(controller, "copyGeneratedPassword");
            assertEquals("SafePassword1234",
                    javafx.scene.input.Clipboard.getSystemClipboard().getString());
            assertEquals("Contraseña copiada al portapapeles.",
                    field(controller, "statusLabel", Label.class).getText());
            return null;
        });
    }

    @Test
    void configuresBothChooserTypesFromTheCurrentFileLocation() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-chooser");
        Path source = Files.createFile(directory.resolve("source.txt"));
        runOnFxThread(() -> {
            TextField sourceField = field(controller, "sourcePathField", TextField.class);
            sourceField.setText(source.toString());

            FileChooser fileChooser = new FileChooser();
            invoke(controller, "setInitialDirectory",
                    new Class<?>[] {FileChooser.class, TextField[].class},
                    fileChooser, new TextField[] {sourceField});
            assertEquals(directory.toFile(), fileChooser.getInitialDirectory());

            DirectoryChooser directoryChooser = new DirectoryChooser();
            invoke(controller, "setInitialDirectory",
                    new Class<?>[] {DirectoryChooser.class, TextField[].class},
                    directoryChooser, new TextField[] {sourceField});
            assertEquals(directory.toFile(), directoryChooser.getInitialDirectory());

            sourceField.clear();
            FileChooser withoutInitialFolder = new FileChooser();
            invoke(controller, "setInitialDirectory",
                    new Class<?>[] {FileChooser.class, TextField[].class},
                    withoutInitialFolder, new TextField[] {sourceField});
            assertNull(withoutInitialFolder.getInitialDirectory());
            return null;
        });
    }

    @Test
    void displaysInformationalMessagesForHelpAndAbout() throws Exception {
        MainController controller = loadController();
        Stage ownerStage = showControllerWindow(controller);
        invokeWhileDismissingAlert(controller, "showHelp");
        invokeWhileDismissingAlert(controller, "showAbout");
        runOnFxThread(() -> {
            ownerStage.close();
            return null;
        });
    }

    @Test
    void startsApplicationAndLoadsItsFxmlAndStylesheet() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();
            new MainApplication().start(stage);
            assertEquals("Enigma", stage.getTitle());
            assertNotNull(stage.getScene());
            assertEquals(1, stage.getScene().getStylesheets().size());
            stage.close();
            return null;
        });
    }
}
