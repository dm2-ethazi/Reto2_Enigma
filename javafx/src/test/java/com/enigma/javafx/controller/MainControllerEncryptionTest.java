package com.enigma.javafx.controller;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainControllerEncryptionTest extends JavaFxTestSupport {
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
            assertControlsDisabled(controller, true);
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
            stage.show();
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
}
