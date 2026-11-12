package com.enigma.javafx.controller;

import com.enigma.javafx.service.FileService;
import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LargePngPreviewValidationTest extends JavaFxTestSupport {
    private static final long PREVIEW_LIMIT = 850L * 1024 * 1024;
    private static final int BUFFER_SIZE = 64 * 1024;

    @TempDir
    Path temporaryDirectory;

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @EnabledIfSystemProperty(named = "enigma.large-png-test", matches = "true")
    void encryptsDecryptsAndPreviewsAn850MebibytePng() throws Exception {
        Path source = temporaryDirectory.resolve("large.png");
        Path encryptedDirectory = Files.createDirectory(temporaryDirectory.resolve("encrypted"));
        Path decryptedDirectory = Files.createDirectory(temporaryDirectory.resolve("decrypted"));
        writePngWithAncillaryChunk(source, PREVIEW_LIMIT);

        assertEquals(PREVIEW_LIMIT, Files.size(source));
        byte[] sourceHash = sha256(source);
        FileService fileService = new FileService();
        long startedAt = System.nanoTime();
        AtomicLong peakHeapBytes = new AtomicLong();
        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "large-png-memory-sampler");
            thread.setDaemon(true);
            return thread;
        });
        sampler.scheduleAtFixedRate(() -> {
            long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            peakHeapBytes.accumulateAndGet(used, Math::max);
        }, 0, 20, TimeUnit.MILLISECONDS);

        try {
            FileService.EncryptionResult encrypted = fileService.encrypt(source, encryptedDirectory);
            Path encryptedFile = encrypted.outputPath();
            assertTrue(Files.size(encryptedFile) > PREVIEW_LIMIT);

            FileService.DecryptionResult decrypted = fileService.decrypt(
                    encryptedFile, decryptedDirectory, encrypted.password().toCharArray());
            Path previewFile = decrypted.outputPath();

            assertEquals(PREVIEW_LIMIT, Files.size(previewFile));
            assertEquals(-1, Files.mismatch(source, previewFile),
                    "El resultado debe coincidir byte a byte con el PNG de origen.");
            assertTrue(MainController.isWithinPngPreviewLimit(Files.size(previewFile)));
            assertArrayEquals(sourceHash, sha256(previewFile),
                    "SHA-256 debe coincidir tras cifrar y descifrar el PNG grande.");
            assertPngHasSafeDimensions(previewFile);

            Image image = loadPngPreview(previewFile);
            assertFalse(image.isError(), () -> String.valueOf(image.getException()));
            assertTrue(image.getWidth() > 0);
            assertTrue(image.getHeight() > 0);

            double elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
            double peakHeapMebibytes = peakHeapBytes.get() / (1024.0 * 1024.0);
            System.out.printf(
                    "Large PNG validation: size=%d MiB, elapsed=%.2f s, sampledPeakHeap=%.2f MiB%n",
                    PREVIEW_LIMIT / (1024 * 1024), elapsedSeconds, peakHeapMebibytes);
        } finally {
            sampler.shutdownNow();
        }
    }

    private static Image loadPngPreview(Path path) throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        Image image = runOnFxThread(() ->
                new Image(path.toUri().toString(), 360, 220, true, true, true));

        runOnFxThread(() -> {
            image.progressProperty().addListener((observable, previous, current) -> {
                if (current.doubleValue() >= 1.0) {
                    loaded.countDown();
                }
            });
            image.errorProperty().addListener((observable, previous, current) -> {
                if (current) {
                    loaded.countDown();
                }
            });
            if (image.isError() || image.getProgress() >= 1.0) {
                loaded.countDown();
            }
            return null;
        });

        assertTrue(loaded.await(60, TimeUnit.SECONDS),
                "La vista previa JavaFX no terminó de cargar en 60 segundos.");
        return image;
    }

    private static void assertPngHasSafeDimensions(Path path) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            assertTrue(input != null, "ImageIO debe abrir el PNG descifrado.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            assertTrue(readers.hasNext(), "Debe existir un lector para el PNG descifrado.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                assertEquals(1, reader.getWidth(0));
                assertEquals(1, reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
    }

    private static void writePngWithAncillaryChunk(Path destination, long totalSize)
            throws IOException {
        BufferedImage pixel = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        pixel.setRGB(0, 0, 0xff336699);
        byte[] png;
        try (ByteArrayOutputStream imageBytes = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(pixel, "png", imageBytes));
            png = imageBytes.toByteArray();
        }

        byte[] endChunk = new byte[12];
        System.arraycopy(png, png.length - endChunk.length, endChunk, 0, endChunk.length);
        int prefixLength = png.length - endChunk.length;
        long chunkLength = totalSize - prefixLength - endChunk.length - 12L;
        if (chunkLength < 0 || chunkLength > Integer.MAX_VALUE) {
            throw new IOException("The requested PNG test size cannot be represented by one PNG chunk.");
        }

        byte[] chunkType = "vpAg".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] zeros = new byte[BUFFER_SIZE];
        CRC32 crc = new CRC32();
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                Files.newOutputStream(destination), BUFFER_SIZE))) {
            output.write(png, 0, prefixLength);
            output.writeInt((int) chunkLength);
            output.write(chunkType);
            crc.update(chunkType);

            long remaining = chunkLength;
            while (remaining > 0) {
                int count = (int) Math.min(remaining, zeros.length);
                output.write(zeros, 0, count);
                crc.update(zeros, 0, count);
                remaining -= count;
            }

            output.writeInt((int) crc.getValue());
            output.write(endChunk);
        }
    }

    private static byte[] sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[BUFFER_SIZE];
        try (var input = Files.newInputStream(path)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        return digest.digest();
    }
}
