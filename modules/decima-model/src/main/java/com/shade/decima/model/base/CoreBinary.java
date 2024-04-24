package com.shade.decima.model.base;

import com.shade.decima.model.rtti.*;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.java.RTTIExtends;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public record CoreBinary(@NotNull List<RTTIObject> objects) implements RTTICoreFile {
    public record Reader(@NotNull RTTIFactory factory) implements RTTICoreFileReader, RTTIBinaryReader {
        @NotNull
        @Override
        public RTTICoreFile read(@NotNull InputStream is, @NotNull ErrorHandlingStrategy errorHandlingStrategy) throws IOException {
            final List<RTTIObject> objects = new ArrayList<>();

            final ByteBuffer header = ByteBuffer
                .allocate(12)
                .order(ByteOrder.LITTLE_ENDIAN);

            while (true) {
                final int read = is.read(header.array());

                if (read <= 0) {
                    break;
                }

                final long hash;
                final ByteBuffer data;

                if (read != header.limit()) {
                    errorHandlingStrategy.handle(new IOException("Unexpected end of stream while reading object header"));
                    continue;
                }

                hash = header.getLong(0);
                data = ByteBuffer.allocate(header.getInt(8)).order(ByteOrder.LITTLE_ENDIAN);

                if (is.read(data.array()) != data.capacity()) {
                    errorHandlingStrategy.handle(new IOException("Unexpected end of stream while reading object data"));
                    continue;
                }

                final RTTIClass type = factory.find(hash);

                if (type == null) {
                    errorHandlingStrategy.handle(new IllegalArgumentException("Can't find type with hash %018x in the registry".formatted(hash)));
                    continue;
                }

                RTTIObject object = null;

                try {
                    object = read(type, factory, data);
                } catch (Exception e) {
                    errorHandlingStrategy.handle(new IllegalArgumentException("Failed to construct object of type " + type, e));
                }

                if (object == null || data.remaining() > 0) {
                    object = InvalidObject.read(factory, this, data.position(0), hash);
                }

                objects.add(object);
            }

            return new CoreBinary(objects);
        }

        @NotNull
        @Override
        public byte[] write(@NotNull RTTICoreFile file) {
            final int size = file.objects().stream().mapToInt(obj -> obj.type().getSize(factory, obj) + 12).sum();
            final var data = new byte[size];
            final var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

            for (RTTIObject entry : file.objects()) {
                final RTTIClass type = entry.type();

                buffer.putLong(factory.getHash(type));
                buffer.putInt(type.getSize(factory, entry));
                type.write(factory, buffer, entry);
            }

            return data;
        }

        @NotNull
        @Override
        public <T> T read(@NotNull RTTIType<T> type, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
            return type.read(factory, this, buffer);
        }
    }

    @Override
    public <T> void visitAllObjects(@NotNull Class<T> type, @NotNull Consumer<T> consumer) {
        visitAllObjects(type::isInstance, consumer);
    }

    @Override
    public void visitAllObjects(@NotNull String type, @NotNull Consumer<RTTIObject> consumer) {
        visitAllObjects(value -> value instanceof RTTIObject object && object.type().isInstanceOf(type), consumer);
    }

    private <T> void visitAllObjects(@NotNull Predicate<Object> predicate, @NotNull Consumer<T> consumer) {
        for (RTTIObject entry : objects) {
            visitAllObjects(entry, predicate, consumer);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void visitAllObjects(@Nullable Object parent, @NotNull Predicate<Object> predicate, @NotNull Consumer<T> consumer) {
        if (parent == null) {
            return;
        }

        if (predicate.test(parent)) {
            consumer.accept((T) parent);
        } else if (parent instanceof RTTIObject object) {
            for (RTTIClass.Field<?> field : object.type().getFields()) {
                visitAllObjects(field.get(object), predicate, consumer);
            }
        } else if (parent instanceof Object[] array) {
            for (Object element : array) {
                visitAllObjects(element, predicate, consumer);
            }
        }
    }

    @RTTIExtends(@Type(name = "RTTIRefObject"))
    public static class InvalidObject {
        @RTTIField(type = @Type(name = "GGUUID"), name = "ObjectUUID")
        public RTTIObject uuid;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;
        @RTTIField(type = @Type(name = "uint64"))
        public long type;

        @NotNull
        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer, long type) {
            final InvalidObject entry = new InvalidObject();
            entry.uuid = factory.<RTTIClass>find("GGUUID").read(factory, reader, buffer);
            entry.data = BufferUtils.getBytes(buffer, buffer.remaining());
            entry.type = type;

            return new RTTIObject(factory.find(InvalidObject.class), entry);
        }
    }
}
