package com.shade.decima.game.until_dawn;

import com.shade.decima.game.until_dawn.rtti.UntilDawn;
import com.shade.decima.rtti.factory.AbstractTypeFactory;
import com.shade.decima.rtti.factory.TypeId;
import com.shade.decima.rtti.runtime.TypeInfo;
import com.shade.util.NotNull;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UntilDawnTypeFactory extends AbstractTypeFactory {
    private static final MethodHandles.Lookup lookup;

    static {
        try {
            lookup = MethodHandles.privateLookupIn(UntilDawn.class, MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public UntilDawnTypeFactory() {
        super(UntilDawn.class, lookup);
    }

    @NotNull
    @Override
    protected TypeId computeTypeId(@NotNull TypeInfo info) {
        return new UntilDawnTypeId(info.name().fullName());
    }

    @Override
    protected void sortSerializableAttrs(@NotNull List<OrderedAttr> attrs) {
        // This is a broken implementation of quicksort used by older versions of Decima
        quicksort(attrs, Comparator.comparingInt(OrderedAttr::offset), 0, attrs.size() - 1, 0);
    }

    @Override
    protected void filterSerializableAttrs(@NotNull List<OrderedAttr> attrs) {
        // Remove save state attribute
        attrs.removeIf(attr -> (attr.info().flags() & 2) != 0);
        // Remove non-"serializable" attributes. They include holders for MsgReadBinary data
        attrs.removeIf(attr -> !attr.serializable());
    }

    private static <T> int quicksort(@NotNull List<T> items, @NotNull Comparator<T> comparator, int left, int right, int state) {
        if (left < right) {
            state = 0x19660D * state + 0x3C6EF35F;

            int pivot = (state >>> 8) % (right - left);
            Collections.swap(items, left + pivot, left);

            int start = partition(items, comparator, left, right);
            state = quicksort(items, comparator, left, start - 1, state);
            state = quicksort(items, comparator, start + 1, right, state);
        }

        return state;
    }

    private static <T> int partition(@NotNull List<T> items, @NotNull Comparator<T> comparator, int left, int right) {
        var l = left - 1;
        var r = right;

        while (true) {
            do {
                if (l >= r) {
                    break;
                }
                l++;
            } while (comparator.compare(items.get(l), items.get(right)) < 0);

            do {
                if (r <= l) {
                    break;
                }
                r--;
            } while (comparator.compare(items.get(right), items.get(r)) < 0);

            if (l >= r) {
                break;
            }

            Collections.swap(items, l, r);
        }

        Collections.swap(items, l, right);

        return l;
    }
}
