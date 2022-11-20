package com.shade.decima.ui.controls.hex;

public interface HexModel {
    int getLength();

    byte getByte(int index);

    short getShort(int index);

    int getInt(int index);

    long getLong(int index);

    float getFloat(int index);

    double getDouble(int index);
}
