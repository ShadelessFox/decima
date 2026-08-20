package com.shade.decima.model.archive.dsar;

import com.shade.decima.model.archive.ArchiveFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HZDRArchiveManagerTest {
    @TempDir
    private Path directory;

    @Test
    void readsFileAcrossChunks() throws IOException {
        final String filePath = "levels/game.core";
        final long hash = HZDRArchiveManager.hashPath(filePath);
        final byte[] data = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

        writeArchive("base.bin", data, 8);
        writeLocators(List.of(new LocatorArchive("base.bin", List.of(new LocatorFile(hash, 5, 8)))));

        try (HZDRArchiveManager manager = new HZDRArchiveManager(directory)) {
            final ArchiveFile file = manager.getFile("LEVELS\\GAME.CORE");
            assertEquals(8, file.getLength());
            assertArrayEquals("56789abc".getBytes(StandardCharsets.UTF_8), file.readAllBytes());
            assertEquals(1, manager.getArchives().size());
            assertEquals(1, manager.getArchives().iterator().next().getFiles().size());
        }
    }

    @Test
    void higherNamedArchiveHasPriority() throws IOException {
        final long hash = HZDRArchiveManager.hashPath("test.core");
        writeArchive("base.bin", new byte[]{1}, 1);
        writeArchive("patch.bin", new byte[]{2}, 1);
        writeLocators(List.of(
            new LocatorArchive("patch.bin", List.of(new LocatorFile(hash, 0, 1))),
            new LocatorArchive("base.bin", List.of(new LocatorFile(hash, 0, 1)))
        ));

        try (HZDRArchiveManager manager = new HZDRArchiveManager(directory)) {
            assertArrayEquals(new byte[]{2}, manager.getFile(hash).readAllBytes());
        }
    }

    @Test
    void rejectsUnsupportedArchiveVersion() throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt('D' | 'S' << 8 | 'A' << 16 | 'R' << 24);
        buffer.putShort((short) 3);
        buffer.putShort((short) 2);
        buffer.putInt(0);
        buffer.putInt(32);
        buffer.putLong(0);
        buffer.putLong(0);

        Files.write(directory.resolve("base.bin"), buffer.array());
        writeLocators(List.of(new LocatorArchive("base.bin", List.of())));

        final IOException error = assertThrows(IOException.class, () -> new HZDRArchiveManager(directory));
        assertTrue(error.getMessage().contains("Unsupported DirectStorage archive version"));
    }

    private void writeArchive(String name, byte[] data, int chunkSize) throws IOException {
        final int chunkCount = (data.length + chunkSize - 1) / chunkSize;
        final byte[][] compressed = new byte[chunkCount][];
        int totalCompressedSize = 0;

        for (int i = 0; i < chunkCount; i++) {
            final int offset = i * chunkSize;
            final int length = Math.min(chunkSize, data.length - offset);
            compressed[i] = compressLiteral(data, offset, length);
            totalCompressedSize += compressed[i].length;
        }

        final int firstChunkOffset = 32 + chunkCount * 32;
        final ByteBuffer buffer = ByteBuffer
            .allocate(firstChunkOffset + totalCompressedSize)
            .order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt('D' | 'S' << 8 | 'A' << 16 | 'R' << 24);
        buffer.putShort((short) 3);
        buffer.putShort((short) 1);
        buffer.putInt(chunkCount);
        buffer.putInt(firstChunkOffset);
        buffer.putLong(data.length);
        buffer.putLong(0);

        int compressedOffset = firstChunkOffset;
        for (int i = 0; i < chunkCount; i++) {
            final int offset = i * chunkSize;
            final int length = Math.min(chunkSize, data.length - offset);
            buffer.putLong(offset);
            buffer.putLong(compressedOffset);
            buffer.putInt(length);
            buffer.putInt(compressed[i].length);
            buffer.put((byte) 3);
            buffer.put(new byte[7]);
            compressedOffset += compressed[i].length;
        }
        for (byte[] chunk : compressed) {
            buffer.put(chunk);
        }

        Files.write(directory.resolve(name), buffer.array());
    }

    private void writeLocators(List<LocatorArchive> archives) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeInt(output, archives.size());

        for (LocatorArchive archive : archives) {
            final byte[] name = archive.name().getBytes(StandardCharsets.UTF_8);
            writeInt(output, name.length);
            output.write(name);
            writeInt(output, archive.files().size());

            for (LocatorFile file : archive.files()) {
                writeLong(output, file.hash());
                writeInt(output, file.offset());
                writeInt(output, file.length());
            }
        }

        Files.write(directory.resolve("PackFileLocators.bin"), output.toByteArray());
    }

    private static byte[] compressLiteral(byte[] data, int offset, int length) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(Math.min(length, 15) << 4);

        if (length >= 15) {
            int remaining = length - 15;
            while (remaining >= 255) {
                output.write(255);
                remaining -= 255;
            }
            output.write(remaining);
        }

        output.write(data, offset, length);
        return output.toByteArray();
    }

    private static void writeInt(ByteArrayOutputStream output, long value) {
        output.write((int) value);
        output.write((int) (value >>> 8));
        output.write((int) (value >>> 16));
        output.write((int) (value >>> 24));
    }

    private static void writeLong(ByteArrayOutputStream output, long value) {
        writeInt(output, value);
        writeInt(output, value >>> 32);
    }

    private record LocatorArchive(String name, List<LocatorFile> files) {}

    private record LocatorFile(long hash, int offset, int length) {}
}
