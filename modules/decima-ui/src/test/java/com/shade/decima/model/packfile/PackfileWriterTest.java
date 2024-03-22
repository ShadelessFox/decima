package com.shade.decima.model.packfile;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.packfile.resource.BufferResource;
import com.shade.decima.model.util.Oodle;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.security.SecureRandom;

import static java.nio.file.StandardOpenOption.*;

public class PackfileWriterTest {
    private static final Logger log = LoggerFactory.getLogger(PackfileWriterTest.class);
    private static final int FILES_COUNT = 3;

    private static Oodle oodle;

    @BeforeAll
    public static void setUp() {
        final ProjectContainer[] projects = ProjectManager.getInstance().getProjects();

        if (projects.length == 0) {
            log.error("Can't find any suitable projects to borrow compressor from");
        } else {
            oodle = Oodle.acquire(projects[0].getCompressorPath());
            log.info("Using compressor " + oodle);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0x0, 0x1, 0x10, 0x100, 0x1000, 0x10000, 0x3ffff, 0x40000, 0x40001, 0x7ffff, 0x80000, 0x80001, 0x7fffff})
    public void writePackfileTest(int length) throws IOException {
        Assumptions.assumeTrue(oodle != null, "Can't find a compressor");

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

                final long written = writer.write(monitor, channel, oodle, new PackfileWriter.Options(Oodle.CompressionLevel.FAST, false));

                channel.position(0);
                channel.truncate(written);
            }
        }

        final var manager = new PackfileManager(oodle);

        try (Packfile packfile = manager.openPackfile(file)) {
            Assertions.assertEquals(FILES_COUNT, packfile.getFileEntries().size());

            for (int i = 0; i < FILES_COUNT; i++) {
                Assertions.assertArrayEquals(files[i], packfile.extract(i));
            }
        }
    }
}
