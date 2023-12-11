package com.shade.platform.ui.controls;

import com.shade.util.Nullable;

import javax.swing.*;

public class VerticalLabel extends JLabel {
    private boolean clockwise;

    public VerticalLabel(@Nullable String text) {
        super(text);
    }

    @Override
    public String getUIClassID() {
        return "VerticalLabelUI";
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public void setClockwise(boolean clockwise) {
        this.clockwise = clockwise;
    }
}
