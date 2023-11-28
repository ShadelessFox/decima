package com.shade.decima.ui.data.viewer.texture.exporter;

import com.shade.decima.ui.data.viewer.texture.TextureExporter;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.util.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

public class TextureExporterPNG implements TextureExporter {
    @Override
    public void export(@NotNull ImageProvider provider, @NotNull Set<Option> options, @NotNull WritableByteChannel channel) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final BufferedImage concatImage = new BufferedImage(provider.getMaxWidth(), provider.getMaxHeight() * provider.getSliceCount(0), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = concatImage.createGraphics();
        for (int slice = 0; slice < provider.getSliceCount(0); slice++) {
            g2d.setComposite(AlphaComposite.Src);
            g2d.drawImage(provider.getImage(0, slice), 0, provider.getMaxHeight() * slice, null);
        }
        g2d.dispose();
        ImageIO.write(concatImage, "png", baos);

        final ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
        channel.write(buffer);
    }

    @Override
    public boolean supportsImage(@NotNull ImageProvider provider) {
        return provider.getType() != ImageProvider.Type.CUBEMAP && provider.getBitsPerChannel() == 8;
    }

    @Override
    public boolean supportsOption(@NotNull Option option) {
        return false;
    }

    @NotNull
    @Override
    public String getExtension() {
        return "png";
    }
}
