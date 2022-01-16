package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

public final class RTTITypeClass implements RTTIType<RTTIObject> {
    private final String name;
    private final Base[] bases;
    private final Member[] members;
    private final int flags;
    private final int unknownFlags;

    public RTTITypeClass(@NotNull String name, @NotNull Base[] bases, @NotNull Member[] members, int flags, int unknownFlags) {
        this.name = name;
        this.bases = bases;
        this.members = members;
        this.flags = flags;
        this.unknownFlags = unknownFlags;
    }

    @NotNull
    @Override
    public RTTIObject read(@NotNull ByteBuffer buffer) {
        final Map<Member, Object> values = new LinkedHashMap<>();
        for (MemberInfo info : getOrderedMembers()) {
            values.put(info.member(), info.member().type().read(buffer));
        }
        return new RTTIObject(this, values);
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        for (MemberInfo info : getOrderedMembers()) {
            info.member().type().write(buffer, object.getMemberValue(info.member()));
        }
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
        quickSort(items, new int[]{0}, new int[]{items.size() - 1}, new int[]{0}, comparator);
    }

    private static <T> void quickSort(@NotNull List<T> items, @NotNull int[] left, @NotNull int[] right, @NotNull int[] seed, @NotNull Comparator<T> comparator) {
        if (left[0] >= right[0]) {
            return;
        }

        seed[0] = 0x19660D * seed[0] + 0x3C6EF35F;
        final int pivot = (seed[0] >>> 8) % (right[0] - left[0]);

        swap(items, left[0] + pivot, right[0]);

        int start = left[0] - 1;
        int end = right[0];

        while (start < end) {
            do {
                start++;
                if (comparator.compare(items.get(start), items.get(right[0])) >= 0) {
                    break;
                }
            } while (start < end);

            if (end <= start) {
                break;
            }

            do {
                end--;
                if (comparator.compare(items.get(right[0]), items.get(end)) >= 0) {
                    break;
                }
            } while (end > start);

            if (start >= end) {
                break;
            }

            swap(items, end, start);
        }

        swap(items, start, right[0]);

        final int[] newRight = {start - 1};
        quickSort(items, left, newRight, seed, comparator);

        final int[] newLeft = {start + 1};
        quickSort(items, newLeft, right, seed, comparator);
    }

    private static <T> void swap(@NotNull List<T> arr, int a, int b) {
        final T tmp = arr.get(a);
        arr.set(a, arr.get(b));
        arr.set(b, tmp);
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
