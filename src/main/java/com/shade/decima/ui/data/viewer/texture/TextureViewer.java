package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReader;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReaderProvider;
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
        final Packfile packfile = ((PropertyEditor) editor).getInput().getNode().getPackfile();
        final RTTIObject header = value.get("Header");

        final TextureViewerPanel panel = (TextureViewerPanel) component;
        panel.setStatusText("%sx%s (%s, %s)".formatted(header.get("Width"), header.get("Height"), header.get("Type"), header.get("PixelFormat")));

        final ImageReaderProvider imageReaderProvider = getImageReaderProvider(header.get("PixelFormat").toString());
        final ImageProvider imageProvider = imageReaderProvider != null ? new MyImageProvider(value, packfile, imageReaderProvider) : null;

        SwingUtilities.invokeLater(() -> {
            panel.getImagePanel().setProvider(imageProvider);
            panel.getImagePanel().fit();
            panel.revalidate();
        });
    }

    @Nullable
    private static ImageReaderProvider getImageReaderProvider(@NotNull String format) {
        for (ImageReaderProvider provider : ServiceLoader.load(ImageReaderProvider.class)) {
            if (provider.supports(format)) {
                return provider;
            }
        }

        return null;
    }

    private static class MyImageProvider implements ImageProvider {
        private final RTTIObject object;
        private final Packfile packfile;
        private final ImageReaderProvider readerProvider;

        public MyImageProvider(@NotNull RTTIObject object, @NotNull Packfile packfile, @NotNull ImageReaderProvider readerProvider) {
            this.object = object;
            this.packfile = packfile;
            this.readerProvider = readerProvider;
        }

        @NotNull
        @Override
        public Image getImage(int mip, int slice) {
            final int mipCount = getMipCount();
            final int sliceCount = getSliceCount();

            Objects.checkIndex(mip, mipCount);
            Objects.checkIndex(slice, sliceCount);

            final RTTIObject header = object.get("Header");
            final RTTIObject data = object.get("Data");
            final int externalMipCount = data.i32("ExternalMipCount");

            final Dimension dimension = new Dimension(header.i16("Width"), header.i16("Height"));
            final ImageReader reader = readerProvider.create(header.get("PixelFormat").toString());

            final Dimension mipDimension = getTextureDimension(reader, dimension, mip);
            final int mipLength = getTextureSize(reader, dimension, mip);
            final int mipOffset;

            final ByteBuffer mipBuffer;

            if (mip < externalMipCount) {
                final RTTIObject dataSource = data.get("ExternalDataSource");
                final String dataSourceLocation = dataSource.get("Location");
                final int dataSourceOffset = dataSource.get("Offset");
                final int dataSourceLength = dataSource.get("Length");
                final byte[] stream;

                try {
                    stream = packfile.extract("%s.core.stream".formatted(dataSourceLocation));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                mipBuffer = ByteBuffer.wrap(stream).slice(dataSourceOffset, dataSourceLength);
                mipOffset = IntStream.range(0, mip + 1)
                    .map(x -> getTextureSize(reader, dimension, x) * (x == mip ? slice : sliceCount))
                    .sum();
            } else {
                mipBuffer = ByteBuffer.wrap(IOUtils.unbox(data.get("InternalData")));
                mipOffset = IntStream.range(externalMipCount, mip + 1)
                    .map(x -> getTextureSize(reader, dimension, x) * (x == mip ? slice : sliceCount))
                    .sum();
            }

            return reader.read(
                mipBuffer.slice(mipOffset, mipLength).order(ByteOrder.LITTLE_ENDIAN),
                mipDimension.width,
                mipDimension.height
            );
        }

        @Override
        public int getMaxWidth() {
            return object.obj("Header").i16("Width");
        }

        @Override
        public int getMaxHeight() {
            return object.obj("Header").i16("Height");
        }

        @Override
        public int getMipCount() {
            return object.obj("Header").i8("TotalMipCount");
        }

        @Override
        public int getSliceCount() {
            final RTTIObject header = object.get("Header");
            return switch (header.get("Type").toString()) {
                case "2D" -> 1;
                case "3D" -> 1 << header.i16("Depth");
                case "2DArray" -> header.i16("Depth");
                case "CubeMap" -> 6;
                default -> throw new IllegalArgumentException("Unsupported texture type");
            };
        }

        @NotNull
        private static Dimension getTextureDimension(@NotNull ImageReader reader, @NotNull Dimension dimension, int mip) {
            return new Dimension(
                Math.max(dimension.width >> mip, reader.getBlockSize()),
                Math.max(dimension.height >> mip, reader.getBlockSize())
            );
        }

        private static int getTextureSize(@NotNull ImageReader reader, @NotNull Dimension dimension, int mip) {
            final Dimension scaled = getTextureDimension(reader, dimension, mip);
            return scaled.width * scaled.height * reader.getPixelBits() / 8;
        }
    }
}
