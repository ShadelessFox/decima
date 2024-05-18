package com.shade.decima.model.packfile;

import com.shade.decima.model.packfile.resource.BufferResource;
import com.shade.decima.model.util.Compressor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.util.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.security.SecureRandom;

import static java.nio.file.StandardOpenOption.*;

public class PackfileWriterTest {
    private static final int FILES_COUNT = 3;

    @ParameterizedTest
    @ValueSource(ints = {0x0, 0x1, 0x10, 0x100, 0x1000, 0x10000, 0x3ffff, 0x40000, 0x40001, 0x7ffff, 0x80000, 0x80001, 0x7fffff})
    public void writePackfileTest(int length) throws IOException {
        final var file = Files.createTempFile("decima", ".bin");
        final var monitor = new VoidProgressMonitor();
        final var files = new byte[FILES_COUNT][length];
        final var random = new SecureRandom();

        try (SeekableByteChannel channel = Files.newByteChannel(file, READ, WRITE, CREATE, TRUNCATE_EXISTING)) {
            try (PackfileWriter writer = new PackfileWriter()) {
                for (int i = 0; i < FILES_COUNT; i++) {
                    random.nextBytes(files[i]);
                    writer.add(new BufferResource(files[i], i));
                }

                final long written = writer.write(monitor, channel, NoOpCompressor.INSTANCE, new PackfileWriter.Options(Compressor.Level.FAST, false));

                channel.position(0);
                channel.truncate(written);
            }
        }

        final var manager = new PackfileManager(NoOpCompressor.INSTANCE);

        try (Packfile packfile = manager.openPackfile(file)) {
            Assertions.assertEquals(FILES_COUNT, packfile.getFileEntries().size());

            for (int i = 0; i < FILES_COUNT; i++) {
                Assertions.assertArrayEquals(files[i], packfile.extract(i));
            }
        }
    }

    private static class NoOpCompressor implements Compressor {
        private static final NoOpCompressor INSTANCE = new NoOpCompressor();

        @NotNull
        @Override
        public ByteBuffer compress(@NotNull ByteBuffer src, @NotNull Level level) {
            return src;
        }

        @Override
        public void decompress(@NotNull ByteBuffer src, @NotNull ByteBuffer dst) {
            dst.put(src);
        }
    }
}
