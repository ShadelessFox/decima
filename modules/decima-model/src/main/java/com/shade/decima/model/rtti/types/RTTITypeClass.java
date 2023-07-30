package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeSerialized;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.Lazy;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

public class RTTITypeClass extends RTTIClass implements RTTITypeSerialized {
    // A special field used for storing an extra data from the MsgReadBinary message without a handler.
    public static final String EXTRA_DATA_FIELD = "ExtraData";

    private final String name;
    private final int version;
    private final int flags;

    private MySuperclass[] superclasses;
    private MyField[] fields;
    private Message<?>[] messages;

    // Cached values
    private Lazy<MyField[]> allFields;
    private Lazy<FieldWithOffset[]> orderedFields;

    public RTTITypeClass(@NotNull String name, int version, int flags) {
        this.name = name;
        this.version = version;
        this.flags = flags;
    }

    @NotNull
    @Override
    public RTTIObject instantiate() {
        final RTTIClass.Message<MessageHandler.ReadBinary> message = getMessage("MsgReadBinary");
        final MessageHandler.ReadBinary handler = message != null ? message.getHandler() : null;

        if (handler != null) {
            throw new IllegalStateException("Can't instantiate a class with the 'MsgReadBinary' message");
        }

        final Map<RTTIClass.Field<?>, Object> values = new HashMap<>();

        for (FieldWithOffset info : getOrderedFields()) {
            values.put(info.field(), info.field().type().instantiate());
        }

        return new RTTIObject(this, values);
    }

    @NotNull
    @Override
    public RTTIObject copyOf(@NotNull RTTIObject value) {
        final RTTIObject instance = instantiate();

        for (FieldWithOffset info : getOrderedFields()) {
            final MyField field = info.field();
            instance.set(field, field.type().copyOf(value.get(field)));
        }

        return instance;
    }

    @NotNull
    @Override
    public RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final Map<RTTIClass.Field<?>, Object> values = new LinkedHashMap<>();
        final RTTIObject object = new RTTIObject(this, values);

        for (FieldWithOffset info : getOrderedFields()) {
            values.put(info.field(), info.field().type().read(registry, buffer));
        }

        final RTTIClass.Message<MessageHandler.ReadBinary> message = getMessage("MsgReadBinary");
        final MessageHandler.ReadBinary handler = message != null ? message.getHandler() : null;

        if (message != null) {
            if (handler != null) {
                handler.read(registry, buffer, object);
            } else {
                object.set(RTTITypeClass.EXTRA_DATA_FIELD, IOUtils.getBytesExact(buffer, buffer.remaining()));
            }
        }

        return object;
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        for (FieldWithOffset info : getOrderedFields()) {
            info.field().type().write(registry, buffer, object.get(info.field()));
        }

        final RTTIClass.Message<MessageHandler.ReadBinary> message = getMessage("MsgReadBinary");
        final MessageHandler.ReadBinary handler = message != null ? message.getHandler() : null;

        if (message != null) {
            if (handler != null) {
                handler.write(registry, buffer, object);
            } else {
                buffer.put(object.<byte[]>get(RTTITypeClass.EXTRA_DATA_FIELD));
            }
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject value) {
        int size = 0;

        for (FieldWithOffset info : getOrderedFields()) {
            if (info.field().isNonReadable()) {
                continue;
            }
            size += info.field().type().getSize(registry, value.get(info.field()));
        }

        final RTTIClass.Message<MessageHandler.ReadBinary> message = getMessage("MsgReadBinary");
        final MessageHandler.ReadBinary handler = message != null ? message.getHandler() : null;

        if (message != null) {
            if (handler != null) {
                size += handler.getSize(registry, value);
            } else {
                size += value.<byte[]>get(RTTITypeClass.EXTRA_DATA_FIELD).length;
            }
        }

        return size;
    }

    @Nullable
    @Override
    public TypeId getTypeId() {
        if (isInstanceOf("RTTIRefObject")) {
            return new RTTITypeDumper().getTypeId(this);
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @NotNull
    @Override
    public MySuperclass[] getSuperclasses() {
        return Objects.requireNonNull(superclasses, "Class is not initialized");
    }

    @NotNull
    @Override
    public MyField[] getDeclaredFields() {
        return Objects.requireNonNull(fields, "Class is not initialized");
    }

    @NotNull
    @Override
    public MyField[] getFields() {
        return Objects.requireNonNull(allFields, "Class is not initialized").get();
    }

    @NotNull
    public FieldWithOffset[] getOrderedFields() {
        return Objects.requireNonNull(orderedFields, "Class is not initialized").get();
    }

    @NotNull
    @Override
    public Message<?>[] getMessages() {
        return Objects.requireNonNull(messages, "Class is not initialized");
    }

    public int getVersion() {
        return version;
    }

    public int getFlags() {
        return flags;
    }

    public void setSuperclasses(@NotNull MySuperclass[] superclasses) {
        this.superclasses = superclasses;
    }

    public void setFields(@NotNull MyField[] fields) {
        this.fields = fields;
        this.allFields = Lazy.of(() -> getOrderedFields(true, true).stream().map(FieldWithOffset::field).toArray(MyField[]::new));
        this.orderedFields = Lazy.of(() -> getOrderedFields(false, false).toArray(FieldWithOffset[]::new));
    }

    public void setMessages(@NotNull Message<?>[] messages) {
        this.messages = messages;
    }

    @NotNull
    private List<FieldWithOffset> getOrderedFields(boolean includeNonHashable, boolean includeNonReadable) {
        final List<FieldWithOffset> fields = new ArrayList<>();
        collectFields(fields, this, 0);
        filterFields(fields, includeNonHashable, includeNonReadable);
        reorderFields(fields);
        return fields;
    }

    private static void collectFields(@NotNull List<FieldWithOffset> fields, @NotNull RTTITypeClass cls, int offset) {
        for (MySuperclass superclass : cls.getSuperclasses()) {
            collectFields(fields, superclass.type(), superclass.offset() + offset);
        }
        for (MyField field : cls.getDeclaredFields()) {
            fields.add(new FieldWithOffset(field, field.offset() + offset));
        }
    }

    private static void reorderFields(@NotNull List<FieldWithOffset> fields) {
        quickSort(fields, Comparator.comparingInt(FieldWithOffset::offset));
    }

    private static void filterFields(@NotNull List<FieldWithOffset> fields, boolean includeNonHashable, boolean includeNonReadable) {
        fields.removeIf(info -> info.field().isSaveState() || (!includeNonHashable && info.field().isNonHashable()) || (!includeNonReadable && info.field().isNonReadable()));
    }

    private static <T> void quickSort(@NotNull List<T> items, @NotNull Comparator<T> comparator) {
        quickSort(items, comparator, 0, items.size() - 1, 0);
    }

    private static <T> int quickSort(@NotNull List<T> items, @NotNull Comparator<T> comparator, int left, int right, int state) {
        if (left < right) {
            state = 0x19660D * state + 0x3C6EF35F;

            final int pivot = (state >>> 8) % (right - left);
            swap(items, left + pivot, right);

            final int start = partition(items, comparator, left, right);
            state = quickSort(items, comparator, left, start - 1, state);
            state = quickSort(items, comparator, start + 1, right, state);
        }

        return state;
    }

    private static <T> int partition(@NotNull List<T> items, @NotNull Comparator<T> comparator, int left, int right) {
        int start = left - 1;
        int end = right;

        while (true) {
            do {
                start++;
            } while (start < end && comparator.compare(items.get(start), items.get(right)) < 0);

            do {
                end--;
            } while (end > start && comparator.compare(items.get(right), items.get(end)) < 0);

            if (start >= end) {
                break;
            }

            swap(items, start, end);
        }

        swap(items, start, right);

        return start;
    }

    private static <T> void swap(@NotNull List<T> items, int a, int b) {
        final T item = items.get(a);
        items.set(a, items.get(b));
        items.set(b, item);
    }

    public record MySuperclass(@NotNull RTTITypeClass type, int offset) implements Superclass {
        @NotNull
        @Override
        public RTTIClass getType() {
            return type;
        }
    }

    public record MyField(@NotNull RTTITypeClass parent, @NotNull RTTIType<?> type, @NotNull String name, @Nullable String category, int offset, int flags) implements Field<Object> {
        public static final int FLAG_SAVE_STATE = 1 << 1;
        public static final int FLAG_NON_HASHABLE = 1 << 31;
        public static final int FLAG_NON_READABLE = 1 << 30;

        private boolean isSaveState() {
            return (flags & FLAG_SAVE_STATE) != 0;
        }

        private boolean isNonHashable() {
            return (flags & FLAG_NON_HASHABLE) != 0;
        }

        private boolean isNonReadable() {
            return (flags & FLAG_NON_READABLE) != 0;
        }

        @NotNull
        @Override
        public Object get(@NotNull RTTIObject object) {
            return object.<Map<MyField, Object>>cast().get(this);
        }

        @Override
        public void set(@NotNull RTTIObject object, Object value) {
            object.<Map<MyField, Object>>cast().put(this, value);
        }

        @NotNull
        @Override
        public RTTIClass getParent() {
            return parent;
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public RTTIType<Object> getType() {
            return (RTTIType<Object>) type;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @Nullable
        @Override
        public String getCategory() {
            return category;
        }
    }

    public record FieldWithOffset(@NotNull MyField field, int offset) {
    }
}
