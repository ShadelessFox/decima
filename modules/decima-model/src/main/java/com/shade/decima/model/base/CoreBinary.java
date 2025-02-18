package com.shade.decima.model.base;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTICoreFileReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.RTTIExtends;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public record CoreBinary(@NotNull List<RTTIObject> objects) implements RTTICoreFile {
    public record Reader(@NotNull RTTITypeRegistry registry) implements RTTICoreFileReader {
        private static final Logger log = LoggerFactory.getLogger(Reader.class);

        @NotNull
        @Override
        public RTTICoreFile read(@NotNull InputStream is, @NotNull ErrorHandlingStrategy handler) throws IOException {
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
                    handler.handle(new IOException("Unexpected end of stream while reading object header"));
                    continue;
                }

                hash = header.getLong(0);
                data = ByteBuffer.allocate(header.getInt(8)).order(ByteOrder.LITTLE_ENDIAN);

                if (is.read(data.array()) != data.capacity()) {
                    handler.handle(new IOException("Unexpected end of stream while reading object data"));
                    continue;
                }

                RTTIClass type = registry.find(hash);
                RTTIObject object = null;

                if (type != null) {
                    try {
                        object = type.read(registry, data);
                    } catch (Exception e) {
                        handler.handle(new IllegalArgumentException("Failed to construct object of type " + type, e));
                    }
                } else {
                    handler.handle(new IllegalArgumentException("Can't find type with hash %016x in the registry".formatted(hash)));
                }

                if (object == null || data.remaining() > 0) {
                    object = InvalidObject.read(registry, data.position(0), hash);
                }

                objects.add(object);
            }

            return new CoreBinary(objects);
        }

        @NotNull
        @Override
        public byte[] write(@NotNull RTTICoreFile file) {
            final int size = file.objects().stream().mapToInt(obj -> obj.type().getSize(registry, obj) + 12).sum();
            final var data = new byte[size];
            final var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

            for (RTTIObject entry : file.objects()) {
                final RTTIClass type = entry.type();

                buffer.putLong(registry.getHash(type));
                buffer.putInt(type.getSize(registry, entry));
                type.write(registry, buffer, entry);
            }

            return data;
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
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, long type) {
            final InvalidObject entry = new InvalidObject();
            entry.uuid = registry.<RTTIClass>find("GGUUID").read(registry, buffer);
            entry.data = BufferUtils.getBytes(buffer, buffer.remaining());
            entry.type = type;

            return new RTTIObject(registry.find(InvalidObject.class), entry);
        }
    }
}
