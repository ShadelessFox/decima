package com.shade.decima.game.hrzr.rtti;

import com.shade.decima.rtti.factory.AbstractTypeFactory;
import com.shade.decima.rtti.factory.TypeId;
import com.shade.decima.rtti.factory.TypeName;
import com.shade.decima.rtti.runtime.TypeInfo;
import com.shade.util.NotNull;
import com.shade.util.hash.HashFunction;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HorizonTypeFactory extends AbstractTypeFactory {
    public HorizonTypeFactory() {
        super(HorizonZeroDawnRemastered.class, MethodHandles.lookup());
    }

    @NotNull
    @Override
    protected TypeId computeTypeId(@NotNull TypeInfo info) {
        var name = getInternalName(info.name());
        var hash = HashFunction.murmur3().hash(name).asLong();
        return HRZRTypeId.of(hash);
    }

    @Override
    protected void sortSerializableAttrs(@NotNull List<OrderedAttr> attrs) {
        quicksort(attrs, Comparator.comparingInt(OrderedAttr::offset), 0, attrs.size() - 1, 0);
    }

    @Override
    protected void filterSerializableAttrs(@NotNull List<OrderedAttr> attrs) {
        // Remove save state attribute
        attrs.removeIf(attr -> (attr.info().flags() & 2) != 0);
        // Remove non-"serializable" attributes. They include holders for MsgReadBinary data
        attrs.removeIf(attr -> !attr.serializable());
    }

    @NotNull
    private static String getInternalName(@NotNull TypeName name) {
        return switch (name) {
            case TypeName.Simple(var n) -> n;
            case TypeName.Parameterized(var n, var a) -> n + '_' + getInternalName(a);
        };
    }

    private static <T> int quicksort(@NotNull List<T> items, @NotNull Comparator<T> comparator, int left, int right, int state) {
        if (left < right) {
            state = 0x19660D * state + 0x3C6EF35F;

            final int pivot = (state >>> 8) % (right - left);
            Collections.swap(items, left + pivot, right);

            final int start = partition(items, comparator, left, right);
            state = quicksort(items, comparator, left, start - 1, state);
            state = quicksort(items, comparator, start + 1, right, state);
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

            Collections.swap(items, start, end);
        }

        Collections.swap(items, start, right);

        return start;
    }
}
