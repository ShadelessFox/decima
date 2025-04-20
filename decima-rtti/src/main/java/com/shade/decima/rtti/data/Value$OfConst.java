package com.shade.decima.rtti.data;

record Value$OfConst<T extends Enum<T> & Value<T>>(int value) implements Value.OfEnum<T> {
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
