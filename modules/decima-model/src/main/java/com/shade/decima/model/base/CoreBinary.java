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
    public record Reader(@NotNull RTTIFactory factory) implements RTTIBinaryReader, RTTICoreFileReader {
        @NotNull
        @Override
        public RTTICoreFile read(@NotNull InputStream is, boolean lenient) throws IOException {
            final List<RTTIObject> objects = new ArrayList<>();

            final ByteBuffer header = ByteBuffer
                .allocate(12)
                .order(ByteOrder.LITTLE_ENDIAN);

            while (true) {
                final int read = is.read(header.array());

                if (read <= 0) {
                    break;
                }

                final RTTIClass type;
                final ByteBuffer data;

                try {
                    if (read != header.limit()) {
                        if (lenient) {
                            break;
                        } else {
                            throw new IOException("Unexpected end of stream while reading object header");
                        }
                    }

                    final var hash = header.getLong(0);
                    type = factory.find(hash);

                    if (type == null) {
                        if (lenient) {
                            continue;
                        } else {
                            throw new IllegalArgumentException("Can't find type with hash 0x" + Long.toHexString(hash) + " in the registry");
                        }
                    }

                    final var size = header.getInt(8);
                    data = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

                    if (is.read(data.array()) != size) {
                        throw new IOException("Unexpected end of stream while reading object data");
                    }
                } catch (Exception e) {
                    if (!lenient) {
                        throw e;
                    }

                    continue;
                }

                RTTIObject object = null;

                try {
                    object = read(type, factory, data);
                } catch (Exception e) {
                    if (!lenient) {
                        throw e;
                    }
                }

                if (object == null || data.remaining() > 0) {
                    object = InvalidObject.read(factory, this, data.position(0), type.getFullTypeName());
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
        public Object uuid;
        @RTTIField(type = @Type(name = "String"))
        public String type;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;

        @NotNull
        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer, @NotNull String type) {
            final InvalidObject entry = new InvalidObject();
            entry.uuid = factory.find("GGUUID").read(factory, reader, buffer);
            entry.type = type;
            entry.data = BufferUtils.getBytes(buffer, buffer.remaining());

            return new RTTIObject(factory.find(InvalidObject.class), entry);
        }
    }
}
