package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.decima.model.rtti.types.java.HwTextureData;
import com.shade.decima.model.rtti.types.java.HwTextureHeader;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReader;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReaderProvider;
import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.IntStream;

@ValueViewerRegistration({
    @Selector(type = @Type(name = "Texture")),
    @Selector(type = @Type(name = "TextureSetEntry")),
    @Selector(type = @Type(name = "TextureList")),
    @Selector(type = @Type(name = "TextureBindingWithHandle")),
    @Selector(type = @Type(name = "UITexture")),
    @Selector(type = @Type(name = "ImageMapEntry")),
    @Selector(type = @Type(name = "ButtonIcon")),
    @Selector(type = @Type(name = "MenuStreamingTexture")),
    @Selector(type = @Type(type = HwTexture.class))
})
public class TextureViewer implements ValueViewer {
    private static final Logger log = LoggerFactory.getLogger(TextureViewer.class);

    @NotNull
    @Override
    public JComponent createComponent() {
        return new TextureViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final TextureInfo info = Objects.requireNonNull(getTextureInfo(controller));
        final HwTextureHeader header = info.texture.obj("Header").cast();
        final TextureViewerPanel panel = (TextureViewerPanel) component;

        panel.setStatusText("%sx%s (%s, %s)".formatted(
            header.getWidth(), header.getHeight(),
            header.getType(), header.getPixelFormat()
        ));

        SwingUtilities.invokeLater(() -> {
            final ImageProvider provider = getImageProvider(info.texture, controller.getProject().getPackfileManager());
            panel.getCanvas().setProvider(provider);
            // TODO
            // panel.getImagePanel().setProvider(provider, info.channels);
            // panel.getImagePanel().fit();
        });
    }

    @Override
    public boolean canView(@NotNull ValueController<?> controller) {
        return getTextureInfo(controller) != null;
    }

    @Nullable
    public static ImageProvider getImageProvider(RTTIObject value, @NotNull PackfileManager manager) {
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

    @Nullable
    private static TextureInfo getTextureInfo(@NotNull ValueController<?> controller) {
        final RTTIObject object = (RTTIObject) controller.getValue();
        final CoreEditor editor = (CoreEditor) controller.getEditor();

        try {
            return getTextureInfo(object, controller.getProject(), editor.getBinary(), 0);
        } catch (IOException e) {
            log.error("Can't obtain texture from " + object.type());
            return null;
        }
    }

    @Nullable
    public static TextureInfo getTextureInfo(@NotNull RTTIObject object, @NotNull Project project, @NotNull CoreBinary binary, int packedData) throws IOException {
        EnumSet<Channel> channels = null;
        RTTIObject texture = null;

        switch (object.type().getTypeName()) {
            case "TextureList" -> {
                final RTTIObject[] textures = object.objs("Textures");
                texture = textures.length == 1 ? textures[0] : null;
            }
            case "UITexture" -> {
                final RTTIObject bigTexture = object.obj("BigTexture");
                texture = bigTexture != null ? bigTexture : object.obj("SmallTexture");
            }
            case "TextureBindingWithHandle" -> {
                return getTextureInfo(object.ref("TextureResource"), project, binary, object.i32("PackedData"));
            }
            case "TextureSetEntry", "ImageMapEntry", "ButtonIcon", "MenuStreamingTexture" -> {
                return getTextureInfo(object.ref("Texture"), project, binary, 0);
            }
            case "TextureSet" -> {
                for (RTTIObject entry : object.objs("Entries")) {
                    final int packingInfo = entry.i32("PackingInfo");
                    final EnumSet<Channel> channelsInUse = getChannels(packedData, packingInfo);

                    if (!channelsInUse.isEmpty()) {
                        final TextureInfo info = getTextureInfo(entry.ref("Texture"), project, binary, 0);
                        channels = channelsInUse;
                        texture = info != null ? info.texture : null;
                        break;
                    }
                }
            }
            default -> texture = object;
        }

        if (texture == null) {
            return null;
        }

        return new TextureInfo(texture, channels);
    }

    @Nullable
    private static TextureInfo getTextureInfo(@NotNull RTTIReference reference, @NotNull Project project, @NotNull CoreBinary binary, int packedData) throws IOException {
        final RTTIReference.FollowResult result = reference.follow(project, binary);
        return result != null ? getTextureInfo(result.object(), project, result.binary(), packedData) : null;
    }

    @NotNull
    public static EnumSet<Channel> getChannels(int packedData, int packingInfo) {
        final int usage = packedData >> 2 & 15;
        final EnumSet<Channel> channels = EnumSet.noneOf(Channel.class);

        if ((packingInfo & 15) == usage)
            channels.add(Channel.R);
        if ((packingInfo >> 8 & 15) == usage)
            channels.add(Channel.G);
        if ((packingInfo >> 16 & 15) == usage)
            channels.add(Channel.B);
        if ((packingInfo >> 24 & 15) == usage)
            channels.add(Channel.A);

        return channels;
    }

    public record TextureInfo(@NotNull RTTIObject texture, @Nullable EnumSet<Channel> channels) {}

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
