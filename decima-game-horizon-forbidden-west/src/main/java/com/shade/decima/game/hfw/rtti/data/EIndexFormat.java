package com.shade.decima.game.hfw.rtti.data;

import com.shade.decima.rtti.Serializable;
import com.shade.decima.rtti.data.Value;

@Serializable(size = 4)
public enum EIndexFormat implements Value.OfEnum<EIndexFormat> {
    Index16("Index16", 0),
    Index32("Index32", 1);

    private final String name;
    private final int value;

    EIndexFormat(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static EIndexFormat valueOf(int value) {
        return (EIndexFormat) Value.valueOf(EIndexFormat.class, value);
    }

    public int stride() {
        return switch (this) {
            case Index16 -> Short.BYTES;
            case Index32 -> Integer.BYTES;
        };
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
