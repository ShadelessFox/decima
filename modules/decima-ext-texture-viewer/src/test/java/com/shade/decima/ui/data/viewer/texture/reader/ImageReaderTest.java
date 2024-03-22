package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

public class ImageReaderTest {
    @ParameterizedTest
    @CsvSource({
        "sm_bd_st_v00_st_7ec03909_001.png, sm_bd_st_v00_st_7ec03909_001.dds"
    })
    public void testBC1(@NotNull String expectedImagePath, @NotNull String actualImagePath) throws Exception {
        compareImages(expectedImagePath, actualImagePath, new ImageReaderBC1());
    }

    @ParameterizedTest
    @CsvSource({
        "plchldr_st_a5eea790_001.png, plchldr_st_a5eea790_001.dds",
        "plchldr_st_2cad4f69_001.png, plchldr_st_2cad4f69_001.dds"
    })
    public void testBC3(@NotNull String expectedImagePath, @NotNull String actualImagePath) throws Exception {
        compareImages(expectedImagePath, actualImagePath, new ImageReaderBC3());
    }

    @ParameterizedTest
    @CsvSource({
        "cm_spl_b001_v06_st_7e21ef68_001.png, cm_spl_b001_v06_st_7e21ef68_001.dds"
    })
    public void testBC4(@NotNull String expectedImagePath, @NotNull String actualImagePath) throws Exception {
        compareImages(expectedImagePath, actualImagePath, new ImageReaderBC4(false));
    }

    @ParameterizedTest
    @CsvSource({
        "plchldr_st_a45a1899_001.png, plchldr_st_a45a1899_001.dds"
    })
    public void testBC5(@NotNull String expectedImagePath, @NotNull String actualImagePath) throws Exception {
        compareImages(expectedImagePath, actualImagePath, new ImageReaderBC5(false));
    }

    @ParameterizedTest
    @CsvSource({
        "cm_spl_b001_v06_st_fea678e1_001.png, cm_spl_b001_v06_st_fea678e1_001.dds",
        "sm_bd_st_v00_st_587b62cd_001.png, sm_bd_st_v00_st_587b62cd_001.dds"
    })
    public void testBC7(@NotNull String expectedImagePath, @NotNull String actualImagePath) throws Exception {
        compareImages(expectedImagePath, actualImagePath, new ImageReaderBC7());
    }

    private static void compareImages(@NotNull String expectedImagePath, @NotNull String actualImagePath, @NotNull ImageReader reader) throws Exception {
        final BufferedImage expectedImage;
        final BufferedImage actualImage;

        try (InputStream is = ImageReaderTest.class.getResourceAsStream(actualImagePath)) {
            assertNotNull(is, "Could not find resource: %s".formatted(actualImagePath));

            final ByteBuffer buffer = ByteBuffer
                .wrap(is.readAllBytes())
                .order(ByteOrder.LITTLE_ENDIAN)
                .position(148);

            actualImage = reader.read(
                buffer,
                buffer.getInt(16),
                buffer.getInt(12)
            );
        }

        try (InputStream is = ImageReaderTest.class.getResourceAsStream(expectedImagePath)) {
            assertNotNull(is, "Could not find resource: %s".formatted(expectedImagePath));

            expectedImage = ImageIO.read(is);
        }

        compareImages(expectedImage, actualImage);
    }

    private static void compareImages(@NotNull BufferedImage expected, @NotNull BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());

        for (int x = 0; x < expected.getWidth(); x++) {
            for (int y = 0; y < expected.getHeight(); y++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), "Pixel at %d-%d should be the same".formatted(x, y));
            }
        }
    }
}
