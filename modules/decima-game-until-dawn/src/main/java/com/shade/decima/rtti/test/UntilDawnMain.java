package com.shade.decima.rtti.test;

import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

public class UntilDawnMain {
    private static final LZ4SafeDecompressor DECOMPRESSOR = LZ4Factory.fastestInstance().safeDecompressor();
    private static final Logger log = LoggerFactory.getLogger(UntilDawnMain.class);

    public static void main(String[] args) throws IOException {
        Path cache = Path.of("D:/PlayStation Games/CUSA02636/localcachepink");
        Path lump = cache.resolve("lumps/levels.game.thegame.leveldescription_lump_09284582-5872-4300-9a7f-ffd251413a48.core");

        var buffer = unpack(lump);
        var reader = new UntilDawnReader(buffer);
        var objects = reader.read();

        if (false) {
            Files.walkFileTree(cache.resolve("lumps"), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String filename = file.getFileName().toString();
                    if (!filename.endsWith(".core")) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (filename.equals("assets_description.lightingsetups_lightingsetups.core")) {
                        log.info("Skipping {}", file);
                        return FileVisitResult.CONTINUE;
                    }
                    log.info("Reading {}", file);
                    var buffer = unpack(file);
                    var reader = new UntilDawnReader(buffer);
                    var objects = reader.read();
                    log.info("Read {} objects", objects.size());

                    return FileVisitResult.CONTINUE;
                }
            });
        }

        // var meshes = objects.stream()
        //     .filter(MeshResourceBase.class::isInstance)
        //     .map(MeshResourceBase.class::cast)
        //     .toList();
        //
        // if (!meshes.isEmpty()) {
        //     var path = Path.of("samples", "until_dawn", lump.getFileName().toString());
        //     Files.createDirectories(path);
        //
        //     for (int i = 0; i < meshes.size(); i++) {
        //         var mesh = meshes.get(i);
        //         System.out.printf("[%d] Exporting %s%n", i, mesh.meshName());
        //
        //         UntilDawnExporter.exportToFile(mesh, mesh.meshName() + '_' + i, path);
        //     }
        // }
    }

    @NotNull
    private static ByteBuffer unpack(@NotNull Path input) throws IOException {
        try (SeekableByteChannel src = Files.newByteChannel(input)) {
            var header = BufferUtils.readFromChannel(src, 32);
            if (header.getInt() != 0xCB10C183) {
                throw new IllegalStateException("Invalid compressed core file header");
            }
            var fileChunkSize = header.getInt();
            var fileTotalSize = header.getLong();
            var checksum = BufferUtils.getLongs(header, 2);

            var chunkCount = Math.toIntExact((fileTotalSize + fileChunkSize - 1) / fileChunkSize);
            var chunkSizes = new int[chunkCount];

            BufferUtils.readFromChannel(src, chunkCount * Integer.BYTES)
                .asIntBuffer()
                .get(chunkSizes);

            ByteBuffer buffer = ByteBuffer
                .allocate(Math.toIntExact(fileTotalSize))
                .order(ByteOrder.LITTLE_ENDIAN);

            for (int chunkSize : chunkSizes) {
                var compressed = BufferUtils.readFromChannel(src, chunkSize);
                var decompressed = ByteBuffer.allocate(fileChunkSize);
                DECOMPRESSOR.decompress(compressed, decompressed);
                buffer.put(decompressed.flip());
            }

            if (!Arrays.equals(MurmurHash3.mmh3(buffer.array()), checksum)) {
                throw new IllegalStateException("Checksum mismatch");
            }

            return buffer.flip();
        }
    }
}
