package com.shade.decima.app.tools;

import com.shade.decima.game.until_dawn.rtti.UntilDawn.WWiseSoundBankResource;
import com.shade.decima.game.until_dawn.rtti.UntilDawnTypeFactory;
import com.shade.decima.game.until_dawn.rtti.UntilDawnTypeReader;
import com.shade.util.io.BinaryReader;
import com.shade.util.io.CompressedBinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class UntilDawnSoundBanksExporter {
    private static final Logger log = LoggerFactory.getLogger(UntilDawnSoundBanksExporter.class);

    public static void main(String[] args) throws IOException {
        var lumps = Path.of("D:/PlayStation Games/Until Dawn/localcachepink/lumps");
        var output = Path.of("samples/until_dawn/wwise");

        var typeFactory = new UntilDawnTypeFactory();
        var typeReader = new UntilDawnTypeReader();

        var banks = new ArrayList<WWiseSoundBankResource>();

        Files.walkFileTree(lumps, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.info("Scanning file {}", lumps.relativize(file));

                try (var reader = CompressedBinaryReader.open(file)) {
                    // Header
                    reader.skip(32);

                    var typeInfoCount = reader.readInt();
                    var typeInfo = reader.readObjects(typeInfoCount, RTTITypeInfo::read);

                    if (typeInfo.stream().anyMatch(x -> x.name().equals("WWiseSoundBankResource"))) {
                        reader.position(0);
                        typeReader.read(reader, typeFactory).stream()
                            .filter(WWiseSoundBankResource.class::isInstance)
                            .map(WWiseSoundBankResource.class::cast)
                            .forEach(banks::add);
                    }
                } catch (Exception e) {
                    log.error("Failed to read file {}", lumps.relativize(file), e);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        for (int i = 0; i < banks.size(); i++) {
            log.info("Writing {} of {}: {}", i + 1, banks.size(), banks.get(i).soundBanks().getFirst().name());
            var data = banks.get(i).soundBanks().getFirst();
            Files.write(output.resolve("%s.bnk".formatted(data.name())), data.data());
        }
    }

    private record RTTITypeInfo(String name, byte[] hash) {
        static RTTITypeInfo read(BinaryReader reader) throws IOException {
            var name = reader.readString(reader.readInt());
            var hash = reader.readBytes(16);
            return new RTTITypeInfo(name, hash);
        }
    }
}
