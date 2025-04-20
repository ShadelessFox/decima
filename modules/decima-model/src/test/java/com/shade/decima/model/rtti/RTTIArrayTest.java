package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.model.rtti.types.RTTITypeNumber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class RTTIArrayTest {
    private static final RTTITypeArray<Integer> type = new RTTITypeArray<>("Array", new RTTITypeNumber<>("int"));

    @Test
    public void insertTest() {
        assertArrayEquals(new int[]{0}, (int[]) type.insert(new int[0], 0, 0));
        assertArrayEquals(new int[]{0, 1}, (int[]) type.insert(new int[]{0}, 1, 1));
        assertArrayEquals(new int[]{0, 1}, (int[]) type.insert(new int[]{1}, 0, 0));
        assertArrayEquals(new int[]{0, 1, 2}, (int[]) type.insert(new int[]{0, 1}, 2, 2));
        assertArrayEquals(new int[]{0, 1, 2}, (int[]) type.insert(new int[]{0, 2}, 1, 1));
        assertArrayEquals(new int[]{0, 1, 2}, (int[]) type.insert(new int[]{1, 2}, 0, 0));
    }

    @Test
    public void removeTest() {
        assertArrayEquals(new int[0], (int[]) type.remove(new int[]{0}, 0));
        assertArrayEquals(new int[]{0}, (int[]) type.remove(new int[]{0, 1}, 1));
        assertArrayEquals(new int[]{1}, (int[]) type.remove(new int[]{0, 1}, 0));
        assertArrayEquals(new int[]{0, 1}, (int[]) type.remove(new int[]{0, 1, 2}, 2));
        assertArrayEquals(new int[]{0, 2}, (int[]) type.remove(new int[]{0, 1, 2}, 1));
        assertArrayEquals(new int[]{1, 2}, (int[]) type.remove(new int[]{0, 1, 2}, 0));
    }

    @Test
    public void moveTest() {
        assertArrayEquals(new int[]{0}, (int[]) type.move(new int[]{0}, 0, 0));
        assertArrayEquals(new int[]{0, 1}, (int[]) type.move(new int[]{1, 0}, 1, 0));
        assertArrayEquals(new int[]{0, 1}, (int[]) type.move(new int[]{1, 0}, 0, 1));
        assertArrayEquals(new int[]{0, 1, 2}, (int[]) type.move(new int[]{0, 2, 1}, 1, 2));
        assertArrayEquals(new int[]{0, 1, 2}, (int[]) type.move(new int[]{1, 0, 2}, 0, 1));
        assertArrayEquals(new int[]{0, 1, 2}, (int[]) type.move(new int[]{0, 2, 1}, 2, 1));
        assertArrayEquals(new int[]{0, 1, 2}, (int[]) type.move(new int[]{1, 0, 2}, 1, 0));
    }
}
