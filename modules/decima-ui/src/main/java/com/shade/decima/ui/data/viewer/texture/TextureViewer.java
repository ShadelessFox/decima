package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.decima.model.rtti.types.java.HwTextureData;
import com.shade.decima.model.rtti.types.java.HwTextureHeader;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.handlers.custom.PackingInfoHandler;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReader;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReaderProvider;
import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.IntStream;
import java.util.EnumSet;

@ValueViewerRegistration({
    @Type(name = "Texture"),
    @Type(name = "TextureSetEntry"),
    @Type(name = "TextureBindingWithHandle"),
    @Type(name = "UITexture"),
    @Type(name = "TextureList"),
    @Type(type = HwTexture.class)
})
public class TextureViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new TextureViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        RTTIObject value = (RTTIObject) controller.getValue();
        final PackfileManager manager = controller.getProject().getPackfileManager();
        final Project project = controller.getProject();
        final EnumSet<Channel> channels = EnumSet.noneOf(Channel.class);
        CoreBinary binary = ((CoreEditor) controller.getEditor()).getBinary();
        String textureUsageName = "";
        String colorSpace = "";

        try {
            if (value.type().getTypeName().equals("TextureBindingWithHandle")) {
                final RTTIReference textureResourceRef = value.ref("TextureResource");
                final RTTIReference.FollowResult textureResource = textureResourceRef.follow(project, binary);
                final int usageType = value.i32("PackedData") >>> 2 & 0xf;
                textureUsageName = PackingInfoHandler.getPurpose(usageType);
                value = textureResource.object();
                binary = textureResource.binary();
                if (value.type().getTypeName().equals("TextureSet")) {
                    final RTTIObject[] entries = value.get("Entries");

                    for (int i = 0; i < entries.length; i++) {
                        final RTTIObject entry = entries[i];
                        final int usageInfo = entry.i32("PackingInfo");
                        value = entry;
                        channels.addAll(PackingInfoHandler.getChannels(usageInfo, textureUsageName));
                        if (!channels.isEmpty()) {
                            break;
                        }
                    }

                    if (channels.isEmpty()) {
                        JOptionPane.showMessageDialog(
                            Application.getInstance().getFrame(),
                            textureUsageName + " not found in any entry of referenced TextureSet",
                            "No matching Texture found",
                            JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                }
            }
            if (value.type().getTypeName().equals("TextureSetEntry")) {
                final RTTIReference textureSetTextureRef = value.ref("Texture");
                final int usageInfo = value.i32("PackingInfo");
                colorSpace = value.str("ColorSpace");
                if (channels.isEmpty()) {
                    channels.addAll(EnumSet.complementOf(PackingInfoHandler.getChannels(usageInfo, "Invalid")));
                }
                value = textureSetTextureRef.get(project, binary);
            }
            if (value.type().getTypeName().equals("TextureList")) {
                final RTTIObject[] textures = value.objs("Textures");
                if (textures.length > 1) {
                    JOptionPane.showMessageDialog(
                        Application.getInstance().getFrame(),
                        "TextureList contains more than one texture, but only one can be displayed",
                        "Only 1st texture is shown",
                        JOptionPane.WARNING_MESSAGE
                    );
                }
                value = textures[0];
            }
            if (value.type().getTypeName().equals("UITexture")) {
                value = (value.obj("BigTexture") != null) ? value.obj("BigTexture") : value.obj("SmallTexture");
            }
            final HwTextureHeader header = value.obj("Header").cast();
            final RTTIObject texture = value;
            final TextureViewerPanel panel = (TextureViewerPanel) component;

            panel.setStatusText("%s %s %s %sx%s (%s, %s)".formatted(
                textureUsageName.equals("Invalid") ? "" : textureUsageName,
                channels.isEmpty() ? "" : channels.toString(),
                colorSpace,
                header.getWidth(), header.getHeight(),
                header.getType(), header.getPixelFormat()));

            SwingUtilities.invokeLater(() -> {
                if (channels.isEmpty()) {
                    panel.getImagePanel().setProvider(getImageProvider(texture, manager));
                } else {
                    panel.getImagePanel().setProvider(getImageProvider(texture, manager), channels);
                }
                panel.getImagePanel().fit();
                panel.revalidate();
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Nullable
    public static ImageProvider getImageProvider(RTTIObject value, PackfileManager manager) {
        final HwTextureHeader header = value.<RTTIObject>get("Header").cast();
        final HwTextureData data = value.<RTTIObject>get("Data").cast();
        final ImageReaderProvider imageReaderProvider = getImageReaderProvider(header.getPixelFormat());
        return imageReaderProvider != null ? new MyImageProvider(header, data, manager, imageReaderProvider) : null;
    }

    @Nullable
    public static ImageReaderProvider getImageReaderProvider(@NotNull String format) {
        for (ImageReaderProvider provider : ServiceLoader.load(ImageReaderProvider.class)) {
            if (provider.supports(format)) {
                return provider;
            }
        }

        return null;
    }

    private record MyImageProvider(@NotNull HwTextureHeader header, @NotNull HwTextureData data, @NotNull PackfileManager manager, @NotNull ImageReaderProvider readerProvider) implements ImageProvider {
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

            final Dimension dimension = new Dimension(header.getWidth(), header.getHeight());
            final ImageReader reader = readerProvider.create(header.getPixelFormat());

            final Dimension mipDimension = getTextureDimension(reader, dimension, mip);
            final int mipLength = getTextureSize(reader, dimension, mip);
            final int mipOffset;

            final ByteBuffer mipBuffer;

            if (mip < data.getExternalMipCount()) {
                final HwDataSource dataSource = Objects.requireNonNull(data.getExternalData());
                final byte[] stream;

                try {
                    stream = dataSource.getData(manager);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                mipBuffer = ByteBuffer.wrap(stream);
                mipOffset = IntStream.range(0, mip + 1)
                    .map(x -> getTextureSize(reader, dimension, x) * (x == mip ? slice : getSliceCount(x)))
                    .sum();
            } else {
                mipBuffer = ByteBuffer.wrap(data.getInternalData());
                mipOffset = IntStream.range(data.getExternalMipCount(), mip + 1)
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
            return header.getWidth();
        }

        @Override
        public int getMaxHeight() {
            return header.getHeight();
        }

        @Override
        public int getMipCount() {
            return header.getMipCount();
        }

        @Override
        public int getSliceCount(int mip) {
            return switch (header.getType()) {
                case "2D" -> 1;
                case "3D" -> 1 << header.getDepth() - mip;
                case "2DArray" -> header.getDepth();
                case "CubeMap" -> 6;
                default -> throw new IllegalArgumentException("Unsupported texture type");
            };
        }

        @Override
        public int getDepth() {
            if (header.getType().equals("3D")) {
                return 1 << header.getDepth();
            } else {
                return 0;
            }
        }

        @Override
        public int getArraySize() {
            if (header.getType().equals("2DArray")) {
                return header.getDepth();
            } else {
                return 0;
            }
        }

        @Nullable
        @Override
        public String getName() {
            final HwDataSource dataSource = data.getExternalData();

            if (dataSource != null) {
                return IOUtils.getFilename(dataSource.getLocation());
            } else {
                return null;
            }
        }

        @NotNull
        @Override
        public Type getType() {
            return switch (header.getType()) {
                case "2D", "2DArray" -> Type.TEXTURE;
                case "3D" -> Type.VOLUME;
                case "CubeMap" -> Type.CUBEMAP;
                default -> throw new IllegalArgumentException("Unsupported texture type");
            };
        }

        @NotNull
        @Override
        public String getPixelFormat() {
            return header.getPixelFormat();
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
