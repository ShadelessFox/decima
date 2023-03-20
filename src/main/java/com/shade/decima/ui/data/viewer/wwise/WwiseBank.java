package com.shade.decima.ui.data.viewer.wwise;

import com.shade.decima.ui.data.viewer.wwise.data.AkHircNode;
import com.shade.decima.ui.data.viewer.wwise.data.AkMusicTrack;
import com.shade.decima.ui.data.viewer.wwise.data.AkSound;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public record WwiseBank(@NotNull Map<Chunk.Type<?>, Chunk> chunks) {
    public WwiseBank {
        final Chunk.BankHeader header = (Chunk.BankHeader) chunks.get(Chunk.Type.BKHD);

        if (header == null) {
            throw new IllegalStateException("Bank has no header");
        }

        if (header.version() < 135) {
            throw new IllegalStateException("Bank version is not supported: " + header.version());
        }
    }

    @NotNull
    public static WwiseBank read(@NotNull ByteBuffer buffer) {
        final Map<Chunk.Type<?>, Chunk> chunks = new HashMap<>();

        while (buffer.hasRemaining()) {
            final var type = new String(IOUtils.getBytesExact(buffer, 4));
            final var size = buffer.getInt();
            final var data = buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN);

            switch (type) {
                case "BKHD" -> chunks.put(Chunk.Type.BKHD, Chunk.BankHeader.read(data));
                case "DIDX" -> chunks.put(Chunk.Type.DIDX, Chunk.MediaIndex.read(data));
                case "DATA" -> chunks.put(Chunk.Type.DATA, Chunk.Data.read(data));
                case "HIRC" -> chunks.put(Chunk.Type.HIRC, Chunk.Hierarchy.read(data));
                default -> chunks.put(new Chunk.Type<>(type, Chunk.Unknown.class), Chunk.Unknown.read(data));
            }

            buffer.position(buffer.position() + size);
        }

        return new WwiseBank(chunks);
    }

    @NotNull
    public <T extends Chunk> T get(@NotNull Chunk.Type<T> type) {
        final Chunk section = chunks.get(type);

        if (section != null) {
            return type.type().cast(section);
        }

        throw new NoSuchElementException("No such chunk: " + type.id());
    }

    public boolean has(@NotNull Chunk.Type<?> type) {
        return chunks.containsKey(type);
    }

    public sealed interface Chunk {
        record Type<T extends Chunk>(@NotNull String id, @NotNull Class<T> type) {
            public static final Type<BankHeader> BKHD = new Type<>("BKHD", BankHeader.class);
            public static final Type<MediaIndex> DIDX = new Type<>("DIDX", MediaIndex.class);
            public static final Type<Data> DATA = new Type<>("DATA", Data.class);
            public static final Type<Hierarchy> HIRC = new Type<>("HIRC", Hierarchy.class);
        }

        record BankHeader(int version, int id) implements Chunk {
            @NotNull
            public static BankHeader read(@NotNull ByteBuffer buffer) {
                return new BankHeader(
                    buffer.getInt(),
                    buffer.getInt()
                );
            }
        }

        record MediaIndex(@NotNull MediaHeader[] header) implements Chunk {
            public record MediaHeader(int id, int offset, int length) {}

            @NotNull
            public static MediaIndex read(@NotNull ByteBuffer buffer) {
                final var count = buffer.remaining() / 12;
                final var entries = new MediaHeader[count];

                for (int i = 0; i < count; i++) {
                    entries[i] = new MediaHeader(buffer.getInt(), buffer.getInt(), buffer.getInt());
                }

                return new MediaIndex(entries);
            }

            @NotNull
            public MediaHeader get(int id) {
                return Arrays.stream(header)
                    .filter(node -> node.id() == id)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(String.valueOf(id)));
            }
        }

        record Data(@NotNull byte[] data) implements Chunk {
            @NotNull
            public static Data read(@NotNull ByteBuffer buffer) {
                return new Data(IOUtils.getBytesExact(buffer, buffer.remaining()));
            }
        }

        record Hierarchy(@NotNull AkHircNode[] nodes) implements Chunk {
            @NotNull
            public static Hierarchy read(@NotNull ByteBuffer buffer) {
                final AkHircNode[] nodes = new AkHircNode[buffer.getInt()];

                for (int i = 0; i < nodes.length; i++) {
                    final var tag = buffer.get();
                    final var len = buffer.getInt();
                    final var buf = buffer.slice(buffer.position(), len).order(ByteOrder.LITTLE_ENDIAN);
                    final var node = switch (tag) {
                        case 0x02 -> AkSound.read(buf);
                        case 0x0B -> AkMusicTrack.read(buf);
                        default -> new Unknown(tag, buf.getInt());
                    };

                    nodes[i] = node;
                    buffer.position(buffer.position() + len);
                }

                return new Hierarchy(nodes);
            }

            private record Unknown(byte tag, int id) implements AkHircNode {}
        }

        record Unknown(@NotNull byte[] data) implements Chunk {
            @NotNull
            public static Unknown read(@NotNull ByteBuffer buffer) {
                return new Unknown(IOUtils.getBytesExact(buffer, buffer.remaining()));
            }
        }
    }
}
