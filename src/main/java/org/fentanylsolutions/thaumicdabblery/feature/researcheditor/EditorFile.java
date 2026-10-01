package org.fentanylsolutions.thaumicdabblery.feature.researcheditor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/** Optimistic external-edit detection and same-directory atomic replacement. */
public final class EditorFile {

    private final Path file;
    private byte[] expected;

    public EditorFile(Path file) throws IOException {
        this.file = file;
        expected = read();
    }

    private byte[] read() throws IOException {
        return Files.exists(file) ? Files.readAllBytes(file) : null;
    }

    public void write(String text) throws IOException {
        if (!Arrays.equals(expected, read()))
            throw new IOException("File changed outside the editor. Close the book and reload scripts.");
        Files.createDirectories(file.getParent());
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Path temporary = Files.createTempFile(file.getParent(), ".research-editor-", ".tmp");
        try {
            Files.write(temporary, bytes);
            if (!Arrays.equals(expected, read()))
                throw new IOException("File changed outside the editor. Close the book and reload scripts.");
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            expected = bytes;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
