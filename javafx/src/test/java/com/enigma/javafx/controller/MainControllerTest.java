package com.enigma.javafx.controller;

import com.enigma.javafx.MainApplication;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javafx.util.Duration;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la lógica del controlador ejecutadas en el hilo de JavaFX.
 */
class MainControllerTest {
    private static final long FX_TIMEOUT_SECONDS = 15;

    @BeforeAll
    static void startJavaFxToolkit() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.complete(null);
            });
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> {
                Platform.setImplicitExit(false);
                started.complete(null);
            });
        }
        started.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void initializesPasswordVisibilityAndClearsAllInputFields() throws Exception {
        MainController controller = loadController();

        PasswordField maskedPassword = field(controller, "generatedPasswordField");
        TextField visiblePassword = field(controller, "visibleGeneratedPasswordField");
        ToggleButton visibilityToggle = field(controller, "showGeneratedPasswordToggle");
        assertTrue(maskedPassword.isVisible());
        assertFalse(visiblePassword.isVisible());
        assertTrue(visibilityToggle.isDisable());

        runOnFxThread(() -> {
            visibilityToggle.setSelected(true);
            maskedPassword.setText("SafePassword1234");
            field(controller, "sourcePathField", TextField.class).setText("source.txt");
            field(controller, "destinationDirectoryField", TextField.class).setText("output");
            field(controller, "referencePathField", TextField.class).setText("reference.txt");
            field(controller, "decryptPasswordField", PasswordField.class).setText("decrypt");
            field(controller, "visibleDecryptPasswordField", TextField.class).setText("decrypt");
            invoke(controller, "clearSelection");
            return null;
        });

        assertEquals("", maskedPassword.getText());
        assertEquals("", visiblePassword.getText());
        assertFalse(visibilityToggle.isSelected());
        assertTrue(visibilityToggle.isDisable());
        assertEquals("Selección limpiada.", field(controller, "statusLabel", Label.class).getText());
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
    void encryptsSelectedFileAndDisplaysGeneratedPassword() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-encrypt");
        Path source = Files.writeString(directory.resolve("source.txt"), "texto de prueba");
        Path destination = Files.createDirectory(directory.resolve("output"));

        runOnFxThread(() -> {
            field(controller, "sourcePathField", TextField.class).setText(source.toString());
            field(controller, "destinationDirectoryField", TextField.class).setText(destination.toString());
            invoke(controller, "encryptFile");
            return null;
        });

        Object task = field(controller, "encryptionTask");
        Method get = task.getClass().getMethod("get", long.class, TimeUnit.class);
        get.invoke(task, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> null);

        String password = field(controller, "generatedPasswordField", PasswordField.class).getText();
        assertNotNull(password);
        assertTrue(password.matches("[A-Za-z0-9]{16}"));
        assertTrue(Files.exists(destination.resolve("source.txt.enc")));
        assertEquals("Archivo cifrado: " + destination.resolve("source.txt.enc"),
                field(controller, "statusLabel", Label.class).getText());
        assertFalse(field(controller, "showGeneratedPasswordToggle", ToggleButton.class).isDisable());
        assertTrue(Files.readAllBytes(destination.resolve("source.txt.enc")).length > 15);
    }

    @Test
    void encryptsEmptyFileAndReportsSuccessfulCompletion() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-empty-encrypt");
        Path source = Files.createFile(directory.resolve("empty.bin"));
        Path destination = Files.createDirectory(directory.resolve("output"));

        runOnFxThread(() -> {
            field(controller, "sourcePathField", TextField.class).setText(source.toString());
            field(controller, "destinationDirectoryField", TextField.class)
                    .setText(destination.toString());
            invoke(controller, "encryptFile");
            return null;
        });
        Object task = field(controller, "encryptionTask");
        task.getClass().getMethod("get", long.class, TimeUnit.class)
                .invoke(task, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> null);

        assertEquals(53, Files.size(destination.resolve("empty.bin.enc")));
        assertTrue(field(controller, "statusLabel", Label.class).getText()
                .startsWith("Archivo cifrado: "));
    }

    @Test
    void reportsEncryptionFailureAndPreservesAnExistingResult() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-failed-encrypt");
        Path source = Files.writeString(directory.resolve("source.txt"), "texto");
        Path destination = Files.createDirectory(directory.resolve("output"));
        Path existingResult = Files.write(destination.resolve("source.txt.enc"),
                new byte[] {1, 2, 3});

        runOnFxThread(() -> {
            field(controller, "sourcePathField", TextField.class).setText(source.toString());
            field(controller, "destinationDirectoryField", TextField.class)
                    .setText(destination.toString());
            return null;
        });
        invokeWhileDismissingAlert(controller, "encryptFile");
        Object task = field(controller, "encryptionTask");
        try {
            task.getClass().getMethod("get", long.class, TimeUnit.class)
                    .invoke(task, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (java.lang.reflect.InvocationTargetException expectedFailure) {
            assertTrue(expectedFailure.getCause() instanceof java.util.concurrent.ExecutionException);
        }
        runOnFxThread(() -> null);

        assertEquals(3, Files.size(existingResult));
        assertEquals("", field(controller, "generatedPasswordField", PasswordField.class).getText());
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
    void displaysInformationalMessagesForUnimplementedActions() throws Exception {
        MainController controller = loadController();
        invokeWhileDismissingAlert(controller, "showHelp");
        invokeWhileDismissingAlert(controller, "showAbout");
        invokeWhileDismissingAlert(controller, "decryptFile");
    }

    @Test
    void reportsInvalidPathsWithoutStartingEncryption() throws Exception {
        MainController controller = loadController();
        Stage ownerStage = runOnFxThread(() -> {
            TextField sourceField = field(controller, "sourcePathField", TextField.class);
            Parent root = sourceField;
            while (root.getParent() != null) {
                root = root.getParent();
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            return stage;
        });
        runOnFxThread(() -> {
            field(controller, "sourcePathField", TextField.class).setText("\0");
            field(controller, "destinationDirectoryField", TextField.class).setText("output");
            return null;
        });

        invokeWhileDismissingAlert(controller, "encryptFile");

        assertNull(field(controller, "encryptionTask"));
        assertControlsDisabled(controller, false);
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

    private static void invokeWhileDismissingAlert(MainController controller, String methodName)
            throws Exception {
        runOnFxThread(() -> {
            Timeline alertDismissal = new Timeline(new KeyFrame(Duration.millis(100), event -> {
                for (Window window : new ArrayList<>(Window.getWindows())) {
                    if (window.isShowing()) {
                        window.hide();
                    }
                }
            }));
            alertDismissal.setCycleCount(50);
            alertDismissal.play();
            invoke(controller, methodName);
            return null;
        });
    }

    private static MainController loadController() throws Exception {
        return runOnFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(
                    MainControllerTest.class.getResource("/com/enigma/javafx/main-view.fxml"));
            loader.load();
            return loader.getController();
        });
    }

    private static void assertControlsDisabled(MainController controller, boolean disabled)
            throws Exception {
        runOnFxThread(() -> {
            assertEquals(disabled, field(controller, "encryptButton", Button.class).isDisable());
            assertEquals(disabled, field(controller, "chooseSourceButton", Button.class).isDisable());
            assertEquals(disabled, field(controller, "chooseDestinationButton", Button.class).isDisable());
            assertEquals(disabled, field(controller, "copyPasswordButton", Button.class).isDisable());
            assertEquals(disabled, field(controller, "clearSelectionButton", Button.class).isDisable());
            return null;
        });
    }

    private static <T> T runOnFxThread(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes,
                                 Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }

    private static <T> T field(Object target, String name) throws Exception {
        return field(target, name, null);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(target);
        if (type == null) {
            @SuppressWarnings("unchecked")
            T castValue = (T) value;
            return castValue;
        }
        return type.cast(value);
    }
}
