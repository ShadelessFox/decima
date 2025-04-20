package com.shade.decima.ui.data.viewer.audio.data.echo;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.*;

public record EchoBank(@NotNull Map<Chunk.Type<?>, Chunk> chunks) {
    private static final int ECHO_MAGIC = 'E' | 'C' << 8 | 'H' << 16 | 'O' << 24;
    private static final int MEDA_MAGIC = 'M' | 'E' << 8 | 'D' << 16 | 'A' << 24;
    private static final int STRL_MAGIC = 'S' | 'T' << 8 | 'R' << 16 | 'L' << 24;
    private static final int PICD_MAGIC = 'P' | 'I' << 8 | 'C' << 16 | 'D' << 24;

    public static EchoBank read(@NotNull ByteBuffer buffer) {
        if (buffer.getInt() != ECHO_MAGIC || buffer.getInt() != -1) {
            throw new IllegalArgumentException("Invalid bank format");
        }

        int size = buffer.getInt();
        Map<Chunk.Type<?>, Chunk> chunks = new HashMap<>();

        for (int i = 0; i < size / 12; i++) {
            int magic = buffer.getInt();
            int offset = buffer.getInt();
            int length = buffer.getInt();

            if (offset > buffer.limit()) {
                throw new IllegalArgumentException("Invalid bank offset");
            }

            ByteBuffer data = buffer.slice(offset, length).order(buffer.order());

            switch (magic) {
                case MEDA_MAGIC -> chunks.put(Chunk.Type.MEDA, Chunk.Media.read(data));
                case STRL_MAGIC -> chunks.put(Chunk.Type.STRL, Chunk.Names.read(data));
            }
        }

        return new EchoBank(Collections.unmodifiableMap(chunks));
    }

    @NotNull
    public <T extends Chunk> T get(@NotNull Chunk.Type<T> type) {
        Chunk chunk = chunks.get(type);

        if (chunk != null) {
            return type.type().cast(chunk);
        }

        throw new NoSuchElementException("No such chunk: " + type.id());
    }

    public sealed interface Chunk {
        record Type<T extends Chunk>(@NotNull String id, @NotNull Class<T> type) {
            public static final Type<Media> MEDA = new Type<>("MEDA", Media.class);
            public static final Type<Names> STRL = new Type<>("STRL", Names.class);
        }

        record Media(@NotNull Entry[] entries) implements Chunk {
            public record Entry(int offset, int size) {
                public static Entry read(@NotNull ByteBuffer buffer) {
                    int offset = Math.toIntExact(buffer.getLong());
                    int size = Math.toIntExact(buffer.getLong());
                    buffer.position(buffer.position() + 32);
                    return new Entry(offset, size);
                }
            }

            public static Media read(@NotNull ByteBuffer buffer) {
                if (buffer.getInt() != PICD_MAGIC) {
                    throw new IllegalArgumentException("Invalid media format");
                }
                int count = buffer.getInt();
                if (buffer.getLong() != 0) {
                    throw new IllegalArgumentException("Expected padding");
                }
                return new Media(BufferUtils.getObjects(buffer, count, Entry[]::new, Entry::read));
            }
        }

        record Names(@NotNull String[] names) implements Chunk {
            @NotNull
            public static Names read(@NotNull ByteBuffer buffer) {
                List<String> names = new ArrayList<>();
                while (buffer.hasRemaining()) {
                    names.add(BufferUtils.getString(buffer));
                }
                return new Names(names.toArray(String[]::new));
            }
        }
    }
}
