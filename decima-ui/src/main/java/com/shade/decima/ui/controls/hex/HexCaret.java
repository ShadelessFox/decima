package com.shade.decima.ui.controls.hex;

import com.shade.util.NotNull;

import javax.swing.event.ChangeListener;

public interface HexCaret {
    void addChangeListener(@NotNull ChangeListener listener);

    void removeChangeListener(@NotNull ChangeListener listener);

    int getDot();

    int getMark();

    void setDot(int dot);

    void moveDot(int dot);
}
