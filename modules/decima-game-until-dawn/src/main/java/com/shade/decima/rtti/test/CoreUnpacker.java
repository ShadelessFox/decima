package com.shade.decima.rtti.test;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.*;

public class CoreUnpacker {
    private static final LZ4SafeDecompressor DECOMPRESSOR = LZ4Factory.safeInstance().safeDecompressor();

    public static void main(String[] args) throws Exception {
        Path cache = Path.of("D:/PlayStation Games/CUSA02636/localcachepink");
        Path lump = cache.resolve("lumps/levels.game.thegame.leveldescription_lump_09284582-5872-4300-9a7f-ffd251413a48.core");

        unpack(lump, Path.of(lump + ".bin"));
    }

    private static void unpack(@NotNull Path input, @NotNull Path output) throws IOException {
        try (
            SeekableByteChannel src = Files.newByteChannel(input);
            SeekableByteChannel dst = Files.newByteChannel(output, CREATE, WRITE, TRUNCATE_EXISTING)
        ) {
            var header = BufferUtils.readFromChannel(src, 32);
            if (header.getInt() != 0xCB10C183) {
                throw new IllegalStateException("Invalid compressed core file header");
            }
            var fileChunkSize = header.getInt();
            var fileTotalSize = header.getLong();
            var uuid = BufferUtils.getBytes(header, 16);

            var chunkCount = Math.toIntExact((fileTotalSize + fileChunkSize - 1) / fileChunkSize);
            var chunkSizes = new int[chunkCount];

            BufferUtils.readFromChannel(src, chunkCount * Integer.BYTES)
                .asIntBuffer()
                .get(chunkSizes);

            for (int chunkSize : chunkSizes) {
                var compressed = BufferUtils.readFromChannel(src, chunkSize);
                var decompressed = ByteBuffer.allocate(fileChunkSize);
                DECOMPRESSOR.decompress(compressed, decompressed);
                dst.write(decompressed.flip());
            }
        }
    }
}
