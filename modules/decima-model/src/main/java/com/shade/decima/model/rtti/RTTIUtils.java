package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

import java.util.Comparator;
import java.util.List;

public class RTTIUtils {
    private RTTIUtils() {
        // prevents instantiation
    }

    @NotNull
    public static String uuidToString(@NotNull RTTIObject o) {
        return "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".formatted(
            o.i8("Data3"), o.i8("Data2"), o.i8("Data1"), o.i8("Data0"),
            o.i8("Data5"), o.i8("Data4"),
            o.i8("Data7"), o.i8("Data6"),
            o.i8("Data8"), o.i8("Data9"),
            o.i8("Data10"), o.i8("Data11"), o.i8("Data12"), o.i8("Data13"), o.i8("Data14"), o.i8("Data15")
        );
    }

    public static <T> void quickSort(@NotNull List<T> items, @NotNull Comparator<T> comparator) {
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
}
