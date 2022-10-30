package com.shade.decima.ui.data.viewer.texture.exporter;

import com.shade.decima.ui.data.viewer.texture.TextureExporter;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.util.NotNull;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

public class TextureExporterPNG implements TextureExporter {
    @Override
    public void export(@NotNull ImageProvider provider, @NotNull Set<Option> options, @NotNull WritableByteChannel channel) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(provider.getImage(0, 0), "png", baos);

        final ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
        channel.write(buffer);
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
        return "png";
    }
}
