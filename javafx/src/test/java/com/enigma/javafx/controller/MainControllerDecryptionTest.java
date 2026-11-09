package com.enigma.javafx.controller;

import com.enigma.javafx.service.FileService;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        Object previewTask = awaitPreviewTask(controller);
        previewTask.getClass().getMethod("get", long.class, TimeUnit.class)
                .invoke(previewTask, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> null);

        Path decryptedFile = destination.resolve(source.getFileName());
        assertEquals(Files.readString(source), Files.readString(decryptedFile));
        assertEquals("Archivo descifrado: " + decryptedFile,
                field(controller, "statusLabel", Label.class).getText());
        assertControlsDisabled(controller, false);
    }

    @Test
    void previewsAuthenticatedTextOnlyAfterClickingDecrypt() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-text-preview");
        Path source = Files.writeString(directory.resolve("source.txt"), "texto de vista previa ñ €");
        Path encryptedDirectory = Files.createDirectory(directory.resolve("encrypted"));
        Path destination = Files.createDirectory(directory.resolve("decrypted"));
        FileService.EncryptionResult encrypted = new FileService().encrypt(source, encryptedDirectory);

        runOnFxThread(() -> {
            field(controller, "decryptSourcePathField", TextField.class)
                    .setText(encrypted.outputPath().toString());
            field(controller, "decryptPasswordField", PasswordField.class)
                    .setText(encrypted.password());
            field(controller, "destinationDirectoryField", TextField.class)
                    .setText(destination.toString());
            assertEquals("", field(controller, "textPreviewArea", TextArea.class).getText());
            assertTrue(field(controller, "previewTask") == null);
            invoke(controller, "decryptFile");
            return null;
        });

        Object decryptionTask = field(controller, "decryptionTask");
        decryptionTask.getClass().getMethod("get", long.class, TimeUnit.class)
                .invoke(decryptionTask, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> null);

        Object task = awaitPreviewTask(controller);
        task.getClass().getMethod("get", long.class, TimeUnit.class)
                .invoke(task, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> null);

        assertEquals(Files.readString(source),
                field(controller, "textPreviewArea", TextArea.class).getText());
        assertTrue(Files.exists(destination.resolve(source.getFileName())));
        assertTrue(field(controller, "previewMessageLabel", Label.class).getText()
                .contains("Contenido completo"));
        assertControlsDisabled(controller, false);

        Object previousTask = field(controller, "previewTask");
        runOnFxThread(() -> {
            ComboBox<?> languageSelector = field(controller, "languageSelector");
            languageSelector.getSelectionModel().select(1);
            languageSelector.fireEvent(new ActionEvent());
            assertEquals(Files.readString(source),
                    field(controller, "textPreviewArea", TextArea.class).getText());
            field(controller, "decryptPasswordField", PasswordField.class).setText("cambio");
            assertEquals("", field(controller, "textPreviewArea", TextArea.class).getText());
            return null;
        });

        assertEquals(previousTask, field(controller, "previewTask"));
    }

    @Test
    void previewsPngFilesAsImages() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-image-preview");
        Path source = directory.resolve("small.png");
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xffff0000);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, "png", bytes));
            Files.write(source, bytes.toByteArray());
        }
        Path encryptedDirectory = Files.createDirectory(directory.resolve("encrypted"));
        Path destination = Files.createDirectory(directory.resolve("decrypted"));
        FileService.EncryptionResult encrypted =
                new FileService().encrypt(source, encryptedDirectory);

        runOnFxThread(() -> {
            field(controller, "decryptSourcePathField", TextField.class)
                    .setText(encrypted.outputPath().toString());
            field(controller, "decryptPasswordField", PasswordField.class)
                    .setText(encrypted.password());
            field(controller, "destinationDirectoryField", TextField.class)
                    .setText(destination.toString());
            invoke(controller, "decryptFile");
            return null;
        });

        Object decryptionTask = field(controller, "decryptionTask");
        decryptionTask.getClass().getMethod("get", long.class, TimeUnit.class)
                .invoke(decryptionTask, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> null);

        Object task = awaitPreviewTask(controller);
        task.getClass().getMethod("get", long.class, TimeUnit.class)
                .invoke(task, FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> null);

        assertNotNull(field(controller, "imagePreview", ImageView.class).getImage(),
                field(controller, "previewMessageLabel", Label.class).getText());
        assertEquals("", field(controller, "textPreviewArea", TextArea.class).getText());
        assertControlsDisabled(controller, false);
    }

    @Test
    void doesNotStartPreviewWhenPasswordIsEnteredWithoutClickingDecrypt() throws Exception {
        MainController controller = loadController();
        Path directory = Files.createTempDirectory("enigma-invalid-preview");
        Path source = Files.writeString(directory.resolve("source.txt"), "texto protegido");
        Path encryptedDirectory = Files.createDirectory(directory.resolve("encrypted"));
        FileService.EncryptionResult encrypted =
                new FileService().encrypt(source, encryptedDirectory);

        runOnFxThread(() -> {
            field(controller, "decryptSourcePathField", TextField.class)
                    .setText(encrypted.outputPath().toString());
            field(controller, "decryptPasswordField", PasswordField.class)
                    .setText("WrongPassword1234");
            return null;
        });

        assertEquals("", field(controller, "textPreviewArea", TextArea.class).getText());
        assertTrue(field(controller, "previewTask") == null);
    }

    private static Object awaitPreviewTask(MainController controller) throws Exception {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(FX_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            Object task = runOnFxThread(() -> field(controller, "previewTask"));
            if (task != null) {
                return task;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("La vista previa no inició después de descifrar el archivo.");
    }
}
