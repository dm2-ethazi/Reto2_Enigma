package com.enigma.javafx.controller;

import com.enigma.javafx.service.FileService;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MainControllerDecryptionTest extends JavaFxTestSupport {
    @Test
    void decryptsSelectedContainerAndReportsOutputPath() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-decrypt");
        Path source = Files.writeString(directory.resolve("source.txt"), "texto para recuperar ñ");
        Path encryptedDirectory = Files.createDirectory(directory.resolve("encrypted"));
        Path destination = Files.createDirectory(directory.resolve("decrypted"));
        FileService fileService = new FileService();
        FileService.EncryptionResult encrypted = fileService.encrypt(source, encryptedDirectory);

        runOnFxThread(() -> {
            field(controller, "sourcePathField", TextField.class)
                    .setText(source.toString());
            field(controller, "decryptSourcePathField", TextField.class)
                    .setText(encrypted.outputPath().toString());
            field(controller, "destinationDirectoryField", TextField.class).setText(destination.toString());
            field(controller, "decryptPasswordField", PasswordField.class).setText(encrypted.password());
            invoke(controller, "decryptFile");
            return null;
        });

        Object task = field(controller, "decryptionTask");
        task.getClass().getMethod("get", long.class, TimeUnit.class)
                .invoke(task, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> null);

        Path decryptedFile = destination.resolve(source.getFileName());
        assertEquals(Files.readString(source), Files.readString(decryptedFile));
        assertEquals("Archivo descifrado: " + decryptedFile,
                field(controller, "statusLabel", Label.class).getText());
        assertControlsDisabled(controller, false);
    }
}
