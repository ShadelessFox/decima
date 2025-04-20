package com.shade.decima.ui.data.viewer.texture.exporter;

import com.shade.decima.ui.data.viewer.texture.TextureExporter;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

public class TextureExporterTIFF implements TextureExporter {
    @Override
    public void export(@NotNull ProgressMonitor monitor, @NotNull ImageProvider provider, @NotNull Set<Option> options, @NotNull WritableByteChannel channel) throws IOException {
        try (var ignored = monitor.begin("Write TIFF")) {
            ImageIO.write(provider.getImage(0, 0), "tiff", Channels.newOutputStream(channel));
        }
    }

    @Override
    public boolean supportsImage(@NotNull ImageProvider provider) {
        return provider.getType() == ImageProvider.Type.TEXTURE;
    }

    @Override
    public boolean supportsOption(@NotNull Option option) {
        return false;
    }


    @NotNull
    @Override
    public String getExtension() {
        return "tiff";
    }
}
