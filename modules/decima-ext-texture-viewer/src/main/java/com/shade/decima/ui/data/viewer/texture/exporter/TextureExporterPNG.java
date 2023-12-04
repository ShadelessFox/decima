package com.shade.decima.ui.data.viewer.texture.exporter;

import com.shade.decima.ui.data.viewer.texture.TextureExporter;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

public class TextureExporterPNG implements TextureExporter {
    @Override
    public void export(@NotNull ProgressMonitor monitor, @NotNull ImageProvider provider, @NotNull Set<Option> options, @NotNull WritableByteChannel channel) throws IOException {
        try (var ignored = monitor.begin("Write PNG")) {
            final BufferedImage image = new BufferedImage(provider.getMaxWidth(), provider.getMaxHeight() * provider.getSliceCount(0), BufferedImage.TYPE_INT_ARGB);
            final Graphics2D graphics = image.createGraphics();

            for (int slice = 0; slice < provider.getSliceCount(0); slice++) {
                graphics.setComposite(AlphaComposite.Src);
                graphics.drawImage(provider.getImage(0, slice), 0, provider.getMaxHeight() * slice, null);
            }

            graphics.dispose();
            ImageIO.write(image, "png", Channels.newOutputStream(channel));
        }
    }

    @Override
    public boolean supportsImage(@NotNull ImageProvider provider) {
        return provider.getType() != ImageProvider.Type.CUBEMAP
            && provider.getImageReader().getColorModel().getPixelSize() <= 32;
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
