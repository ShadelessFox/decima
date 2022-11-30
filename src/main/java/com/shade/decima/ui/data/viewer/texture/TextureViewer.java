package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.messages.impl.TextureHandler.HwTextureData;
import com.shade.decima.model.rtti.messages.impl.TextureHandler.HwTextureHeader;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.JavaObject;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReader;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReaderProvider;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.IntStream;

@ValueViewerRegistration(@Type(name = "Texture", game = GameType.DS))
public class TextureViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new TextureViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final RTTIObject value = (RTTIObject) Objects.requireNonNull(((CoreEditor) editor).getSelectedValue());
        final Packfile packfile = ((CoreEditor) editor).getInput().getNode().getPackfile();
        final HwTextureHeader header = (HwTextureHeader) ((JavaObject) value.get("Header")).object();
        final HwTextureData data = (HwTextureData) ((JavaObject) value.get("Data")).object();

        final TextureViewerPanel panel = (TextureViewerPanel) component;
        panel.setStatusText("%sx%s (%s, %s)".formatted(header.width, header.height, header.type, header.pixelFormat));

        final ImageReaderProvider imageReaderProvider = getImageReaderProvider(header.pixelFormat.toString());
        final ImageProvider imageProvider = imageReaderProvider != null ? new MyImageProvider(header, data, packfile, imageReaderProvider) : null;

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

    private record MyImageProvider(@NotNull HwTextureHeader header, @NotNull HwTextureData data, @NotNull Packfile packfile, @NotNull ImageReaderProvider readerProvider) implements ImageProvider {
        @NotNull
        @Override
        public BufferedImage getImage(int mip, int slice) {
            final ImageData data = getImageData(mip, slice);
            return data.reader.read(data.buffer, data.width, data.height);
        }

        @NotNull
        @Override
        public ByteBuffer getData(int mip, int slice) {
            final ImageData data = getImageData(mip, slice);
            return data.buffer;
        }

        @NotNull
        private ImageData getImageData(int mip, int slice) {
            Objects.checkIndex(mip, getMipCount());
            Objects.checkIndex(slice, getSliceCount(mip));

            final Dimension dimension = new Dimension(header.width, header.height);
            final ImageReader reader = readerProvider.create(header.pixelFormat.toString());

            final Dimension mipDimension = getTextureDimension(reader, dimension, mip);
            final int mipLength = getTextureSize(reader, dimension, mip);
            final int mipOffset;

            final ByteBuffer mipBuffer;

            if (mip < data.externalMipCount) {
                final HwDataSource dataSource = (HwDataSource) ((JavaObject) data.externalDataSource).object();
                final byte[] stream;

                try {
                    stream = packfile.extract("%s.core.stream".formatted(dataSource.location));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                mipBuffer = ByteBuffer.wrap(stream).slice(dataSource.offset, dataSource.length);
                mipOffset = IntStream.range(0, mip + 1)
                    .map(x -> getTextureSize(reader, dimension, x) * (x == mip ? slice : getSliceCount(x)))
                    .sum();
            } else {
                mipBuffer = ByteBuffer.wrap(data.internalData);
                mipOffset = IntStream.range(data.externalMipCount, mip + 1)
                    .map(x -> getTextureSize(reader, dimension, x) * (x == mip ? slice : getSliceCount(x)))
                    .sum();
            }

            return new ImageData(
                reader,
                mipBuffer.slice(mipOffset, mipLength).order(ByteOrder.LITTLE_ENDIAN),
                mipDimension.width,
                mipDimension.height
            );
        }

        @Override
        public int getMaxWidth() {
            return header.width;
        }

        @Override
        public int getMaxHeight() {
            return header.height;
        }

        @Override
        public int getMipCount() {
            return header.totalMipCount;
        }

        @Override
        public int getSliceCount(int mip) {
            return switch (header.type.toString()) {
                case "2D" -> 1;
                case "3D" -> 1 << header.depth - mip;
                case "2DArray" -> header.depth;
                case "CubeMap" -> 6;
                default -> throw new IllegalArgumentException("Unsupported texture type");
            };
        }

        @Override
        public int getDepth() {
            if (header.type.toString().equals("3D")) {
                return 1 << header.depth;
            } else {
                return 0;
            }
        }

        @Override
        public int getArraySize() {
            if (header.type.toString().equals("2DArray")) {
                return header.depth;
            } else {
                return 0;
            }
        }

        @NotNull
        @Override
        public Type getType() {
            return switch (header.type.toString()) {
                case "2D", "2DArray" -> Type.TEXTURE;
                case "3D" -> Type.VOLUME;
                case "CubeMap" -> Type.CUBEMAP;
                default -> throw new IllegalArgumentException("Unsupported texture type");
            };
        }

        @NotNull
        @Override
        public String getPixelFormat() {
            return header.pixelFormat.toString();
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

        private record ImageData(@NotNull ImageReader reader, @NotNull ByteBuffer buffer, int width, int height) {}
    }
}
