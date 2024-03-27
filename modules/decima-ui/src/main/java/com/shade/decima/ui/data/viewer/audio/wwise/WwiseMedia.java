package com.shade.decima.ui.data.viewer.audio.wwise;

import com.shade.decima.ui.data.viewer.audio.AudioPlayerUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public record WwiseMedia(@NotNull Map<Chunk.Type<?>, Chunk> chunks) {
    private static final int RIFF_MAGIC = 'F' << 24 | 'F' << 16 | 'I' << 8 | 'R';
    private static final int WAVE_MAGIC = 'E' << 24 | 'V' << 16 | 'A' << 8 | 'W';
    private static final int FMT_MAGIC = ' ' << 24 | 't' << 16 | 'm' << 8 | 'f';

    @NotNull
    public static WwiseMedia read(@NotNull ByteBuffer buffer) {
        final var riffMagic = buffer.getInt();
        final var dataSize = buffer.getInt();
        final var waveMagic = buffer.getInt();

        if (riffMagic != RIFF_MAGIC || dataSize <= 0 || waveMagic != WAVE_MAGIC) {
            throw new IllegalStateException("Invalid or corrupted WEM file");
        }

        buffer.limit(dataSize + 8);

        final Map<Chunk.Type<?>, Chunk> chunks = new HashMap<>();

        while (buffer.hasRemaining()) {
            final var type = buffer.getInt();
            final var size = buffer.getInt();
            final var data = buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN);

            switch (type) {
                case FMT_MAGIC -> chunks.put(Chunk.Type.FMT, Chunk.Format.read(data));
            }

            buffer.position(buffer.position() + size);
        }

        return new WwiseMedia(chunks);
    }

    @NotNull
    public <T extends Chunk> T get(@NotNull Chunk.Type<T> type) {
        final Chunk section = chunks.get(type);

        if (section != null) {
            return type.type().cast(section);
        }

        throw new NoSuchElementException("No such chunk: " + type.id());
    }

    public sealed interface Chunk {
        record Type<T extends Chunk>(@NotNull String id, @NotNull Class<T> type) {
            public static final Type<Format> FMT = new Type<>("fmt ", Format.class);
        }

        record Format(int codec, int channels, int samplesPerSecond, int bytesPerSecond, int sampleCount) implements Chunk {
            @NotNull
            public static Format read(@NotNull ByteBuffer buffer) {
                final var codec = buffer.getShort() & 0xffff;
                final var channels = buffer.getShort() & 0xffff;
                final var samplesPerSecond = buffer.getInt();
                final var bytesPerSecond = buffer.getInt();
                final var blockAlign = buffer.getShort() & 0xffff;
                final var bitsPerSample = buffer.getShort() & 0xffff;
                final var extraSize = buffer.getShort() & 0xffff;

                if (codec != 0xffff) {
                    throw new IllegalStateException("Codec expected to be 65535, was " + codec);
                }

                if (blockAlign != 0) {
                    throw new IllegalStateException("Block alignment expected to be 0, was " + blockAlign);
                }

                if (bitsPerSample != 0) {
                    throw new IllegalStateException("Bits per sample expected to be 0, was " + bitsPerSample);
                }

                if (extraSize != buffer.remaining()) {
                    throw new IllegalStateException("Extra size expected to be " + buffer.remaining() + ", was " + extraSize);
                }

                if (extraSize >= 6) {
                    buffer.position(buffer.position() + 6);
                } else if (extraSize >= 2) {
                    buffer.position(buffer.position() + 2);
                }

                final var sampleCount = buffer.getInt();

                return new Format(codec, channels, samplesPerSecond, bytesPerSecond, sampleCount);
            }

            @NotNull
            public Duration getDuration() {
                return AudioPlayerUtils.getDuration(sampleCount, samplesPerSecond);
            }
        }
    }
}
