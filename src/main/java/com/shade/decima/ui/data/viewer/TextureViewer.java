package com.shade.decima.ui.data.viewer;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.viewer.texture.TextureReader;
import com.shade.decima.ui.data.viewer.texture.TextureReaderProvider;
import com.shade.decima.ui.editor.property.PropertyEditor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.IntStream;

public class TextureViewer implements ValueViewer {
    public static final TextureViewer INSTANCE = new TextureViewer();

    @NotNull
    @Override
    public JComponent createComponent() {
        return new TextureViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final RTTIObject value = (RTTIObject) Objects.requireNonNull(((PropertyEditor) editor).getSelectedValue());
        final RTTIObject header = value.get("Header");

        final TextureReaderProvider provider = getTextureReaderProvider(header.get("PixelFormat").toString());
        final Image image;

        if (provider != null) {
            try {
                image = getImage(value, 0, provider, ((PropertyEditor) editor).getInput().getNode().getPackfile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            image = null;
        }

        SwingUtilities.invokeLater(() -> {
            final TextureViewerPanel panel = (TextureViewerPanel) component;
            panel.setImage(image);
            panel.setStatusText("%sx%s (%s, %s)".formatted(header.get("Width"), header.get("Height"), header.get("Type"), header.get("PixelFormat")));
        });
    }

    @NotNull
    private static Image getImage(@NotNull RTTIObject object, int mip, @NotNull TextureReaderProvider provider, @NotNull Packfile packfile) throws IOException {
        final RTTIObject header = object.get("Header");
        final RTTIObject data = object.get("Data");
        final int totalMipCount = Objects.checkIndex(mip, header.<Byte>get("TotalMipCount"));
        final int externalMipCount = data.<Integer>get("ExternalMipCount");

        final Dimension dimension = new Dimension(header.<Short>get("Width"), header.<Short>get("Height"));
        final TextureReader reader = provider.create(dimension.width, dimension.height, header.get("PixelFormat").toString());
        final int mipLength = getTextureSize(reader, dimension, mip);
        final int mipOffset;
        final ByteBuffer mipBuffer;

        if (mip < externalMipCount) {
            final RTTIObject dataSource = data.get("ExternalDataSource");
            final String dataSourceLocation = dataSource.get("Location");
            final int dataSourceOffset = dataSource.get("Offset");
            final int dataSourceLength = dataSource.get("Length");
            final byte[] stream = packfile.extract("%s.core.stream".formatted(dataSourceLocation));

            mipOffset = IntStream.range(0, mip).map(x -> getTextureSize(reader, dimension, x)).sum();
            mipBuffer = ByteBuffer.wrap(stream).slice(dataSourceOffset, dataSourceLength);
        } else {
            mipOffset = IntStream.range(0, mip - externalMipCount).map(x -> getTextureSize(reader, dimension, x)).sum();
            mipBuffer = ByteBuffer.wrap(IOUtils.unbox(data.get("InternalData")));
        }

        return reader.read(mipBuffer.slice(mipOffset, mipLength).order(ByteOrder.LITTLE_ENDIAN));
    }

    @NotNull
    private static Dimension getTextureDimension(@NotNull TextureReader reader, @NotNull Dimension dimension, int mip) {
        return new Dimension(
            Math.max(dimension.width >> mip, reader.getBlockSize()),
            Math.max(dimension.height >> mip, reader.getBlockSize())
        );
    }

    private static int getTextureSize(@NotNull TextureReader reader, @NotNull Dimension dimension, int mip) {
        final Dimension scaled = getTextureDimension(reader, dimension, mip);
        return scaled.width * scaled.height * reader.getPixelBits() / 8;
    }

    @Nullable
    private static TextureReaderProvider getTextureReaderProvider(@NotNull String format) {
        for (TextureReaderProvider provider : ServiceLoader.load(TextureReaderProvider.class)) {
            if (provider.supports(format)) {
                return provider;
            }
        }

        return null;
    }
}
