package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeSerialized;
import com.shade.decima.model.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeDumper;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

public class RTTITypeClass extends RTTITypeSerialized<RTTIObject> {
    private final String name;
    private final Base[] bases;
    private final Member[] members;
    private final Map<String, Object> messages;
    private final int flags1;
    private final int flags2;

    public RTTITypeClass(@NotNull String name, @NotNull Base[] bases, @NotNull Member[] members, @NotNull Map<String, Object> messages, int flags1, int flags2) {
        this.name = name;
        this.bases = bases;
        this.members = members;
        this.messages = messages;
        this.flags1 = flags1;
        this.flags2 = flags2;
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

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject value) {
        if (hasMessage("MsgReadBinary")) {
            throw new IllegalStateException("Can't determine size of the class which has MsgReadBinary");
        }

        int size = 0;

        for (MemberInfo info : getOrderedMembers()) {
            size += info.member().type().getSize(registry, value.get(info.member()));
        }

        return size;
    }

    @NotNull
    public RTTIObject instantiate() {
        return new RTTIObject(this, new LinkedHashMap<>());
    }

    public boolean isInstanceOf(@NotNull String type) {
        if (this.getTypeName().equals(type)) {
            return true;
        }

        for (Base base : bases) {
            if (base.type().isInstanceOf(type)) {
                return true;
            }
        }

        return false;
    }

    public boolean isInstanceOf(@NotNull RTTITypeClass cls) {
        if (this == cls) {
            return true;
        }

        for (Base base : bases) {
            if (base.type().isInstanceOf(cls)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public TypeId getTypeId() {
        if (isInstanceOf("CoreObject")) {
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
    public Class<RTTIObject> getInstanceType() {
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
        final Member member = findMember(name);

        if (member != null) {
            return member;
        } else {
            throw new IllegalArgumentException("Type " + getTypeName() + " has no member called '" + name + "'");
        }
    }

    public boolean hasMember(@NotNull String name) {
        return findMember(name) != null;
    }

    @Nullable
    private Member findMember(@NotNull String name) {
        for (Member member : members) {
            if (member.name.equals(name)) {
                return member;
            }
        }

        for (Base base : bases) {
            final Member member = base.type.findMember(name);

            if (member != null) {
                return member;
            }
        }

        return null;
    }

    @NotNull
    public Map<String, Object> getMessages() {
        return messages;
    }

    public boolean hasMessage(@NotNull String name) {
        return messages.containsKey(name);
    }

    public boolean hasMessageHandler(@NotNull String name) {
        return messages.get(name) != null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getMessageHandler(@NotNull String message) {
        return (T) messages.get(message);
    }

    public int getFlags1() {
        return flags1;
    }

    public int getFlags2() {
        return flags2;
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
        return getTypeName();
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

    public record Base(@NotNull RTTITypeClass parent, @NotNull RTTITypeClass type, int offset) {}

    public record Member(@NotNull RTTITypeClass parent, @NotNull RTTIType<?> type, @NotNull String name, @Nullable String category, int offset, int flags) {
        public static final int FLAG_SAVE_STATE = 1 << 1;

        public boolean isSaveState() {
            return (flags & FLAG_SAVE_STATE) > 0;
        }
    }

    public record MemberInfo(@NotNull Member member, int offset) {}
}
