package com.shade.decima.ui.data.viewer.wwise;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public record WwiseBank(@NotNull Map<Section.Type<?>, Section> sections) {
    @NotNull
    public static WwiseBank read(@NotNull ByteBuffer buffer) {
        final Map<Section.Type<?>, Section> sections = new HashMap<>();

        while (buffer.hasRemaining()) {
            final var type = new String(IOUtils.getBytesExact(buffer, 4));
            final var size = buffer.getInt();
            final var data = buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN);

            switch (type) {
                case "DIDX" -> sections.put(Section.Type.DIDX, Section.WemIndex.read(data));
                case "DATA" -> sections.put(Section.Type.DATA, Section.WemData.read(data));
                default -> sections.put(new Section.Type<>(type, Section.Unknown.class), Section.Unknown.read(data));
            }

            buffer.position(buffer.position() + size);
        }

        return new WwiseBank(sections);
    }

    @NotNull
    public <T extends Section> T get(@NotNull Section.Type<T> type) {
        final Section section = sections.get(type);

        if (section != null) {
            return type.type.cast(section);
        }

        throw new IllegalArgumentException("No such section: " + type.id);
    }

    public boolean has(@NotNull Section.Type<?> type) {
        return sections.containsKey(type);
    }

    public sealed interface Section {
        record Type<T extends Section>(@NotNull String id, @NotNull Class<T> type) {
            public static final Type<WemIndex> DIDX = new Type<>("DIDX", WemIndex.class);
            public static final Type<WemData> DATA = new Type<>("DATA", WemData.class);
        }

        record WemIndex(@NotNull Entry[] entries) implements Section {
            public record Entry(int id, int offset, int length) {}

            @NotNull
            public static WemIndex read(@NotNull ByteBuffer buffer) {
                final var count = buffer.remaining() / 12;
                final var entries = new Entry[count];

                for (int i = 0; i < count; i++) {
                    entries[i] = new Entry(buffer.getInt(), buffer.getInt(), buffer.getInt());
                }

                return new WemIndex(entries);
            }
        }

        record WemData(@NotNull byte[] data) implements Section {
            @NotNull
            public static WemData read(@NotNull ByteBuffer buffer) {
                return new WemData(IOUtils.getBytesExact(buffer, buffer.remaining()));
            }
        }

        record Unknown(@NotNull byte[] data) implements Section {
            @NotNull
            public static Unknown read(@NotNull ByteBuffer buffer) {
                return new Unknown(IOUtils.getBytesExact(buffer, buffer.remaining()));
            }
        }
    }
}
