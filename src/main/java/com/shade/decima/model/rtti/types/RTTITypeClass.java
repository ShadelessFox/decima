package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

public final class RTTITypeClass implements RTTIType<RTTIObject> {
    private final String name;
    private final Base[] bases;
    private final Member[] members;
    private final Map<String, Object> messages;
    private final int flags;
    private final int unknownFlags;

    public RTTITypeClass(@NotNull String name, @NotNull Base[] bases, @NotNull Member[] members, @NotNull Map<String, Object> messages, int flags, int unknownFlags) {
        this.name = name;
        this.bases = bases;
        this.members = members;
        this.messages = messages;
        this.flags = flags;
        this.unknownFlags = unknownFlags;
    }

    @NotNull
    @Override
    public RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final Map<Member, Object> values = new LinkedHashMap<>();
        final RTTIObject object = new RTTIObject(this, values);

        for (MemberInfo info : getOrderedMembers()) {
            values.put(info.member(), info.member().type().read(registry, buffer));
        }

        final RTTIMessageReadBinary handler = getMessageHandler("MsgReadBinary");
        if (handler != null) {
            handler.read(registry, object, buffer);
        }

        return object;
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        for (MemberInfo info : getOrderedMembers()) {
            info.member().type().write(registry, buffer, object.get(info.member()));
        }
    }

    @NotNull
    public RTTIObject instantiate() {
        return new RTTIObject(this, new LinkedHashMap<>());
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @NotNull
    @Override
    public Class<RTTIObject> getComponentType() {
        return RTTIObject.class;
    }

    @NotNull
    public Base[] getBases() {
        return bases;
    }

    @NotNull
    public Member[] getMembers() {
        return members;
    }

    @NotNull
    public Member getMember(@NotNull String name) {
        for (Member member : members) {
            if (member.name.equals(name)) {
                return member;
            }
        }

        throw new IllegalArgumentException("Type " + getName() + " has no member called '" + name + "'");
    }

    @NotNull
    public Map<String, Object> getMessages() {
        return messages;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getMessageHandler(@NotNull String message) {
        return (T) messages.get(message);
    }

    public int getFlags() {
        return flags;
    }

    public int getUnknownFlags() {
        return unknownFlags;
    }

    @NotNull
    public List<MemberInfo> getOrderedMembers() {
        final List<MemberInfo> members = new ArrayList<>();
        collectMembers(members, this, 0);
        filterMembers(members);
        reorderMembers(members);
        return members;
    }

    @Override
    public String toString() {
        return getName();
    }

    private static void collectMembers(@NotNull List<MemberInfo> members, @NotNull RTTITypeClass cls, int offset) {
        for (Base base : cls.getBases()) {
            collectMembers(members, base.type(), base.offset() + offset);
        }
        for (Member member : cls.getMembers()) {
            members.add(new MemberInfo(member, member.offset() + offset));
        }
    }

    private static void reorderMembers(@NotNull List<MemberInfo> members) {
        quickSort(members, Comparator.comparingInt(MemberInfo::offset));
    }

    private static void filterMembers(@NotNull List<MemberInfo> members) {
        members.removeIf(info -> info.member().isSaveState());
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

    public static record Base(@NotNull RTTITypeClass parent, @NotNull RTTITypeClass type, int offset) {
    }

    public static record Member(@NotNull RTTITypeClass parent, @NotNull RTTIType<?> type, @NotNull String name, @Nullable String category, int offset, int flags) {
        public static final int FLAG_SAVE_STATE = 1 << 1;

        public boolean isSaveState() {
            return (flags & FLAG_SAVE_STATE) > 0;
        }
    }

    public static record MemberInfo(@NotNull Member member, int offset) {
    }
}
