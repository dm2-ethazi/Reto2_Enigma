package com.enigma.javafx.controller;

import com.enigma.javafx.MainApplication;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

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
                assertFalse(field(controller, "progressIndicator", ProgressIndicator.class).isVisible());
                assertFalse(field(controller, "progressBar", ProgressBar.class).isVisible());
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
                assertEquals(javafx.scene.paint.Color.web("#8c6f47"), showGeneratedIcon.getIconColor());
                assertEquals(javafx.scene.paint.Color.web("#8c6f47"), hideGeneratedIcon.getIconColor());
                assertEquals(javafx.scene.paint.Color.web("#8c6f47"), showDecryptIcon.getIconColor());
                assertEquals(javafx.scene.paint.Color.web("#8c6f47"), hideDecryptIcon.getIconColor());

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
                        field(controller, "textPreviewArea", TextArea.class).setText("contenido previsualizado");
                        invoke(controller, "clearSelection");
                        return null;
                });

                assertEquals("", maskedPassword.getText());
                assertEquals("", visiblePassword.getText());
                assertFalse(visibilityToggle.isSelected());
                assertEquals("", visibilityToggle.getText());
                assertEquals("", decryptVisibilityToggle.getText());
                assertTrue(showDecryptIcon.isVisible());
                assertFalse(hideDecryptIcon.isVisible());
                assertEquals("", field(controller, "decryptSourcePathField", TextField.class).getText());
                assertEquals("", field(controller, "textPreviewArea", TextArea.class).getText());
                assertTrue(visibilityToggle.isDisable());
                assertEquals("Selección limpiada.", field(controller, "statusLabel", Label.class).getText());
        }

        @Test
        void clearsEachOptionsViewWhenLeavingIt() throws Exception {
                MainController controller = loadController();

                runOnFxThread(() -> {
                        invoke(controller, "showEncryptionView");
                        field(controller, "sourcePathField", TextField.class).setText("source.png");
                        field(controller, "destinationDirectoryField", TextField.class).setText("output");
                        field(controller, "generatedPasswordField", PasswordField.class)
                                        .setText("Generated123456");
                        field(controller, "visibleGeneratedPasswordField", TextField.class)
                                        .setText("Generated123456");
                        field(controller, "showGeneratedPasswordToggle", ToggleButton.class).setSelected(true);
                        invoke(controller, "showMainMenu");

                        assertEquals("", field(controller, "sourcePathField", TextField.class).getText());
                        assertEquals("", field(controller, "destinationDirectoryField", TextField.class).getText());
                        assertEquals("", field(controller, "generatedPasswordField", PasswordField.class).getText());
                        assertEquals("", field(controller, "visibleGeneratedPasswordField", TextField.class).getText());
                        assertFalse(field(controller, "showGeneratedPasswordToggle", ToggleButton.class).isSelected());

                        invoke(controller, "showDecryptionView");
                        field(controller, "decryptSourcePathField", TextField.class).setText("source.png.enc");
                        field(controller, "decryptDestinationDirectoryField", TextField.class).setText("output");
                        field(controller, "referencePathField", TextField.class).setText("source.png");
                        field(controller, "decryptPasswordField", PasswordField.class).setText("Decrypt123456");
                        field(controller, "showPasswordToggle", ToggleButton.class).setSelected(true);
                        field(controller, "verifyCheckbox", CheckBox.class).setSelected(true);
                        field(controller, "textPreviewArea", TextArea.class).setText("preview text");
                        field(controller, "imagePreview", ImageView.class).setImage(new WritableImage(1, 1));
                        field(controller, "previewMessageLabel", Label.class).setText("Preview loaded");
                        invoke(controller, "showMainMenu");

                        assertEquals("", field(controller, "decryptSourcePathField", TextField.class).getText());
                        assertEquals("", field(controller, "decryptDestinationDirectoryField", TextField.class).getText());
                        assertEquals("", field(controller, "referencePathField", TextField.class).getText());
                        assertEquals("", field(controller, "decryptPasswordField", PasswordField.class).getText());
                        assertFalse(field(controller, "showPasswordToggle", ToggleButton.class).isSelected());
                        assertFalse(field(controller, "verifyCheckbox", CheckBox.class).isSelected());
                        assertEquals("", field(controller, "textPreviewArea", TextArea.class).getText());
                        assertNull(field(controller, "imagePreview", ImageView.class).getImage());
                        assertEquals("El contenido se mostrará aquí después de pulsar Descifrar archivo.",
                                        field(controller, "previewMessageLabel", Label.class).getText());
                        assertEquals("Listo para trabajar.", field(controller, "statusLabel", Label.class).getText());
                        return null;
                });
        }

        @Test
        void switchesBetweenTheThreeViewsWithoutReplacingTheWindow() throws Exception {
                MainController controller = loadController();
                Node menu = field(controller, "menuView", Node.class);
                Node encryption = field(controller, "encryptionView", Node.class);
                Node decryption = field(controller, "decryptionView", Node.class);

                assertTrue(menu.isVisible());
                assertTrue(menu.isManaged());
                assertFalse(encryption.isVisible());
                assertFalse(decryption.isVisible());

                runOnFxThread(() -> {
                        invoke(controller, "showEncryptionView");
                        return null;
                });
                assertFalse(menu.isVisible());
                assertTrue(encryption.isVisible());
                assertTrue(encryption.isManaged());
                assertFalse(decryption.isVisible());

                runOnFxThread(() -> {
                        invoke(controller, "showDecryptionView");
                        return null;
                });
                assertFalse(menu.isVisible());
                assertFalse(encryption.isVisible());
                assertTrue(decryption.isVisible());
                assertTrue(decryption.isManaged());

                runOnFxThread(() -> {
                        invoke(controller, "showMainMenu");
                        return null;
                });
                assertTrue(menu.isVisible());
                assertFalse(encryption.isVisible());
                assertFalse(decryption.isVisible());
        }

        @Test
        void registersKeyboardShortcutsAndListsThemInHelp() throws Exception {
                MainController controller = loadController();
                Stage stage = showControllerWindow(controller);
                Node menu = field(controller, "menuView", Node.class);
                Node encryption = field(controller, "encryptionView", Node.class);
                Node decryption = field(controller, "decryptionView", Node.class);
                Path directory = Files.createTempDirectory("enigma-keyboard-shortcuts");
                Path source = Files.writeString(directory.resolve("source.txt"), "shortcut encryption test");
                Path destination = Files.createDirectory(directory.resolve("output"));

                try {
                        Callable<Void> keyboardActions = () -> {
                                Scene scene = stage.getScene();
                                invoke(controller, "installKeyboardShortcuts",
                                                new Class<?>[] { Scene.class }, scene);
                                var accelerators = scene.getAccelerators();
                                var encryptionShortcut = new javafx.scene.input.KeyCodeCombination(
                                                javafx.scene.input.KeyCode.DIGIT1,
                                                javafx.scene.input.KeyCombination.CONTROL_DOWN);
                                var decryptionShortcut = new javafx.scene.input.KeyCodeCombination(
                                                javafx.scene.input.KeyCode.DIGIT2,
                                                javafx.scene.input.KeyCombination.CONTROL_DOWN);
                                var runOperationShortcut = new javafx.scene.input.KeyCodeCombination(
                                                javafx.scene.input.KeyCode.ENTER,
                                                javafx.scene.input.KeyCombination.CONTROL_DOWN);
                                var backShortcut = new javafx.scene.input.KeyCodeCombination(
                                                javafx.scene.input.KeyCode.ESCAPE);
                                var helpShortcut = new javafx.scene.input.KeyCodeCombination(
                                                javafx.scene.input.KeyCode.F1);
                                var aboutShortcut = new javafx.scene.input.KeyCodeCombination(
                                                javafx.scene.input.KeyCode.F2);

                                assertNotNull(accelerators.get(encryptionShortcut));
                                assertNotNull(accelerators.get(decryptionShortcut));
                                assertNotNull(accelerators.get(runOperationShortcut));
                                assertNotNull(accelerators.get(backShortcut));
                                assertNotNull(accelerators.get(helpShortcut));
                                assertNotNull(accelerators.get(aboutShortcut));

                                accelerators.get(encryptionShortcut).run();
                                assertTrue(encryption.isVisible());
                                assertFalse(decryption.isVisible());

                                accelerators.get(decryptionShortcut).run();
                                assertFalse(encryption.isVisible());
                                assertTrue(decryption.isVisible());

                                accelerators.get(backShortcut).run();
                                assertTrue(menu.isVisible());
                                assertFalse(encryption.isVisible());
                                assertFalse(decryption.isVisible());
                                accelerators.get(runOperationShortcut).run();
                                assertTrue(menu.isVisible());

                                String spanishHelp = (String) invoke(controller, "helpContent");
                                assertTrue(spanishHelp.contains("Ctrl+Enter: ejecutar"));
                                assertTrue(spanishHelp.contains("Esc: volver"));
                                assertTrue(spanishHelp.contains("F2: mostrar información acerca de la aplicación."));

                                ComboBox<?> languageSelector = field(controller, "languageSelector", ComboBox.class);
                                languageSelector.getSelectionModel().select(1);
                                languageSelector.fireEvent(new ActionEvent());
                                String englishHelp = (String) invoke(controller, "helpContent");
                                assertTrue(englishHelp.contains("Keyboard shortcuts:"));
                                assertTrue(englishHelp.contains("F2: show information about the application."));
                                assertTrue(englishHelp.contains("Ctrl+1: open the encryption view."));
                                assertTrue(englishHelp.contains("Ctrl+Enter: run the operation"));

                                field(controller, "sourcePathField", TextField.class).setText(source.toString());
                                field(controller, "destinationDirectoryField", TextField.class)
                                                .setText(destination.toString());
                                accelerators.get(encryptionShortcut).run();
                                accelerators.get(runOperationShortcut).run();
                                return null;
                        };
                        runOnFxThread(keyboardActions);

                        Object encryptionTask = field(controller, "encryptionTask");
                        encryptionTask.getClass().getMethod("get", long.class, java.util.concurrent.TimeUnit.class)
                                        .invoke(encryptionTask, FX_TIMEOUT_SECONDS,
                                                        java.util.concurrent.TimeUnit.SECONDS);
                        runOnFxThread(() -> null);
                        assertTrue(Files.exists(destination.resolve("source.txt.enc")));
                        assertTrue(field(controller, "generatedPasswordField", PasswordField.class).getText()
                                        .matches("[A-Za-z0-9]{16}"));
                } finally {
                        runOnFxThread(() -> {
                                stage.close();
                                return null;
                        });
                }
        }

        @Test
        void switchesInterfaceLanguageWithoutChangingSelectedPathsOrFileContents() throws Exception {
                MainController controller = loadController();
                Path file = Files.createTempFile("enigma-language", ".txt");
                byte[] originalContents = "Contenido original: ñ, € y 漢字"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                Files.write(file, originalContents);

                runOnFxThread(() -> {
                        ComboBox<?> languageSelector = field(controller, "languageSelector");
                        TextField sourcePath = field(controller, "sourcePathField", TextField.class);
                        TextField destinationPath = field(controller, "destinationDirectoryField", TextField.class);
                        PasswordField password = field(controller, "decryptPasswordField", PasswordField.class);
                        Button encryptButton = field(controller, "encryptButton", Button.class);
                        Label status = field(controller, "statusLabel", Label.class);
                        Label menuTitle = (Label) field(controller, "menuView", Parent.class).lookup(".menu-title");
                        Label previewMessage = field(controller, "previewMessageLabel", Label.class);
                        ToggleButton passwordToggle = field(controller, "showPasswordToggle", ToggleButton.class);
                        ToggleButton generatedPasswordToggle = field(controller, "showGeneratedPasswordToggle",
                                        ToggleButton.class);

                        sourcePath.setText(file.toString());
                        destinationPath.setText(file.getParent().toString());
                        password.setText("KeepThisPassword123");

                        languageSelector.getSelectionModel().select(1);
                        languageSelector.fireEvent(new ActionEvent());
                        assertEquals("Encrypt file", encryptButton.getText());
                        assertEquals("What would you like to do?", menuTitle.getText());
                        assertEquals("The contents will appear here after you click Decrypt file.",
                                        previewMessage.getText());
                        assertEquals("Language", field(controller, "languageLabel", Label.class).getText());
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
                        assertEquals("Zer egin nahi duzu?", menuTitle.getText());
                        assertEquals("Edukia hemen agertuko da Deszifratu fitxategia sakatu ondoren.",
                                        previewMessage.getText());
                        assertEquals("Prest.", status.getText());
                        assertEquals("Arrastatu fitxategi bat hona edo aukeratu", sourcePath.getPromptText());

                        languageSelector.getSelectionModel().select(0);
                        languageSelector.fireEvent(new ActionEvent());
                        assertEquals("Cifrar archivo", encryptButton.getText());
                        assertEquals("¿Qué deseas hacer?", menuTitle.getText());
                        assertEquals("El contenido se mostrará aquí después de pulsar Descifrar archivo.",
                                        previewMessage.getText());
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
                                        new Class<?>[] { String.class, Object[].class },
                                        "Cifrando archivo: {0}%", new Object[] { 42 });
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
                        invoke(controller, "setSourceFile", new Class<?>[] { File.class }, source.toFile());
                        return null;
                });

                assertEquals(source.toAbsolutePath().toString(),
                                field(controller, "sourcePathField", TextField.class).getText());
                assertEquals("",
                                field(controller, "destinationDirectoryField", TextField.class).getText());
                assertEquals("Archivo de entrada seleccionado.",
                                field(controller, "statusLabel", Label.class).getText());

                File selectedDirectory = runOnFxThread(() -> (File) invoke(controller,
                                "findInitialDirectory", new Class<?>[] { TextField[].class },
                                (Object) new TextField[] { field(controller, "sourcePathField") }));
                assertEquals(directory.toFile(), selectedDirectory);

                File noDirectory = runOnFxThread(() -> (File) invoke(controller,
                                "findInitialDirectory", new Class<?>[] { TextField[].class },
                                (Object) new TextField[] { new TextField() }));
                assertNull(noDirectory);

                File selectedFolder = runOnFxThread(() -> {
                        TextField folderField = new TextField(directory.toString());
                        return (File) invoke(controller, "findInitialDirectory",
                                        new Class<?>[] { TextField[].class }, (Object) new TextField[] { folderField });
                });
                assertEquals(directory.toFile(), selectedFolder);
        }

        @Test
        void acceptsArbitraryReadableFilesAsEncryptionSourcesAndEncContainers() throws Exception {
                Path directory = Files.createTempDirectory("enigma-source-types");
                Path text = Files.createFile(directory.resolve("message.TXT"));
                Path image = Files.createFile(directory.resolve("image.png"));
                Path binary = Files.createFile(directory.resolve("archive.pdf"));
                Path encryptedText = Files.createFile(directory.resolve("message.txt.enc"));
                Path encryptedImage = Files.createFile(directory.resolve("image.png.enc"));
                Path encryptedBinary = Files.createFile(directory.resolve("archive.pdf.enc"));

                assertTrue((boolean) invoke(new MainController(), "isSupportedSourceFile",
                                new Class<?>[] { File.class }, text.toFile()));
                assertTrue((boolean) invoke(new MainController(), "isSupportedSourceFile",
                                new Class<?>[] { File.class }, image.toFile()));
                assertFalse((boolean) invoke(new MainController(), "isSupportedSourceFile",
                                new Class<?>[] { File.class }, directory.toFile()));
                assertTrue((boolean) invoke(new MainController(), "isSupportedSourceFile",
                                new Class<?>[] { File.class }, binary.toFile()));
                assertTrue((boolean) invoke(new MainController(), "isSupportedEncryptedContainer",
                                new Class<?>[] { File.class }, encryptedText.toFile()));
                assertTrue((boolean) invoke(new MainController(), "isSupportedEncryptedContainer",
                                new Class<?>[] { File.class }, encryptedImage.toFile()));
                assertTrue((boolean) invoke(new MainController(), "isSupportedEncryptedContainer",
                                new Class<?>[] { File.class }, encryptedBinary.toFile()));
                assertFalse((boolean) invoke(new MainController(), "isSupportedEncryptedContainer",
                                new Class<?>[] { File.class }, binary.toFile()));
        }

        @Test
        void convertsOptionalPathsAndReportsMissingOwnerWindow() throws Exception {
                MainController controller = loadController();
                TextField pathField = field(controller, "sourcePathField");

                Path emptyPath = runOnFxThread(() -> (Path) invoke(controller,
                                "selectedPath", new Class<?>[] { TextField.class }, pathField));
                assertNull(emptyPath);

                Path expected = Path.of("source.txt");
                runOnFxThread(() -> {
                        pathField.setText(expected.toString());
                        return null;
                });
                Path actual = runOnFxThread(() -> (Path) invoke(controller,
                                "selectedPath", new Class<?>[] { TextField.class }, pathField));
                assertEquals(expected, actual);

                Window owner = runOnFxThread(() -> (Window) invoke(controller,
                                "getOwnerWindow", new Class<?>[] { Node.class }, pathField));
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
                                "getOwnerWindow", new Class<?>[] { Node.class }, pathField)));
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
                                        new Class<?>[] { boolean.class }, true);
                        return null;
                });
                assertControlsDisabled(controller, true);

                runOnFxThread(() -> {
                        invoke(controller, "setEncryptionControlsDisabled",
                                        new Class<?>[] { boolean.class }, false);
                        return null;
                });
                assertControlsDisabled(controller, false);
        }

        @Test
        void showsProgressIndicatorsOnlyWhileAnOperationIsRunning() throws Exception {
                MainController controller = loadController();

                runOnFxThread(() -> {
                        invoke(controller, "setOperationProgressVisible", new Class<?>[] { boolean.class }, true);
                        assertTrue(field(controller, "progressIndicator", ProgressIndicator.class).isVisible());
                        assertTrue(field(controller, "progressBar", ProgressBar.class).isVisible());
                        invoke(controller, "setOperationProgressVisible", new Class<?>[] { boolean.class }, false);
                        assertFalse(field(controller, "progressIndicator", ProgressIndicator.class).isVisible());
                        assertFalse(field(controller, "progressBar", ProgressBar.class).isVisible());
                        return null;
                });
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
                                        new Class<?>[] { FileChooser.class, String.class, TextField[].class },
                                        fileChooser, "testFileChooserDirectory", new TextField[] { sourceField });
                        assertEquals(directory.toFile(), fileChooser.getInitialDirectory());

                        DirectoryChooser directoryChooser = new DirectoryChooser();
                        invoke(controller, "setInitialDirectory",
                                        new Class<?>[] { DirectoryChooser.class, String.class, TextField[].class },
                                        directoryChooser, "testDirectoryChooserDirectory",
                                        new TextField[] { sourceField });
                        assertEquals(directory.toFile(), directoryChooser.getInitialDirectory());

                        sourceField.clear();
                        FileChooser withoutInitialFolder = new FileChooser();
                        invoke(controller, "setInitialDirectory",
                                        new Class<?>[] { FileChooser.class, String.class, TextField[].class },
                                        withoutInitialFolder, "testMissingDirectory", new TextField[] { sourceField });
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
                        assertEquals("Gestor Enigma AES", stage.getTitle());
                        assertEquals(1, stage.getIcons().size());
                        assertFalse(stage.getIcons().get(0).isError());
                        assertEquals(stage.getIcons().get(0).getWidth(),
                                        stage.getIcons().get(0).getHeight());
                        assertNotNull(stage.getScene());
                        assertEquals(1, stage.getScene().getStylesheets().size());
                        assertEquals(6, stage.getScene().getAccelerators().size());

                        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                        assertEquals(Math.min(1000.0, screenBounds.getWidth() * 0.90),
                                        stage.getWidth(), 2.0);
                        assertEquals(Math.min(1200.0, screenBounds.getHeight() * 0.95),
                                        stage.getHeight(), 2.0);
                        assertFalse(stage.isMaximized());
                        assertTrue(stage.getWidth() > 0);
                        assertTrue(stage.getHeight() > 0);
                        assertTrue(stage.getWidth() >= Math.min(800.0, screenBounds.getWidth() * 0.90));
                        assertTrue(stage.getHeight() >= Math.min(600.0, screenBounds.getHeight() * 0.95));
                        assertTrue(stage.getWidth() <= screenBounds.getWidth());
                        assertTrue(stage.getHeight() <= screenBounds.getHeight());
                        assertTrue(stage.getX() >= screenBounds.getMinX());
                        assertTrue(stage.getY() >= screenBounds.getMinY());
                        assertTrue(stage.getX() + stage.getWidth() <= screenBounds.getMaxX() + 2.0);
                        assertTrue(stage.getY() + stage.getHeight() <= screenBounds.getMaxY() + 2.0);

                        stage.getScene().getRoot().applyCss();
                        Label encryptionTitle = (Label) stage.getScene().getRoot()
                                        .lookup("#menuEncryptionTitle");
                        Label decryptionTitle = (Label) stage.getScene().getRoot()
                                        .lookup("#menuDecryptionTitle");
                        assertNotNull(encryptionTitle);
                        assertNotNull(decryptionTitle);
                        assertEquals(javafx.scene.paint.Color.web("#0003C9"), encryptionTitle.getTextFill());
                        assertEquals(javafx.scene.paint.Color.web("#C90068"), decryptionTitle.getTextFill());
                        assertEquals(28.0, encryptionTitle.getFont().getSize());
                        assertEquals(28.0, decryptionTitle.getFont().getSize());
                        Button encryptButton = (Button) stage.getScene().getRoot().lookup("#encryptButton");
                        Button encryptionFileButton = (Button) stage.getScene().getRoot()
                                        .lookup("#chooseSourceButton");
                        Button decryptButton = (Button) stage.getScene().getRoot().lookup("#decryptButton");
                        Button decryptionFileButton = (Button) stage.getScene().getRoot()
                                        .lookup("#chooseEncryptedFileButton");
                        assertEquals(javafx.scene.paint.Color.web("#0003C9"), encryptButton.getTextFill());
                        assertEquals(javafx.scene.paint.Color.web("#0003C9"), encryptionFileButton.getTextFill());
                        assertEquals(javafx.scene.paint.Color.web("#C90068"), decryptButton.getTextFill());
                        assertEquals(javafx.scene.paint.Color.web("#C90068"), decryptionFileButton.getTextFill());
                        Node comparisonRow = ((CheckBox) stage.getScene().getRoot()
                                        .lookup("#verifyCheckbox")).getParent();
                        Node decryptButtonRow = decryptButton.getParent();
                        Parent decryptionCard = comparisonRow.getParent();
                        assertEquals(decryptionCard, decryptButtonRow.getParent());
                        var decryptionCardChildren = decryptionCard.getChildrenUnmodifiable();
                        assertEquals(decryptionCardChildren.size() - 1,
                                        decryptionCardChildren.indexOf(decryptButtonRow));
                        stage.close();
                        return null;
                });
        }
}
