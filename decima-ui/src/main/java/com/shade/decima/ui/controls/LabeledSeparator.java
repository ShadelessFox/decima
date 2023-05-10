package com.shade.decima.ui.controls;

import com.shade.decima.ui.controls.plaf.LabeledSeparatorUI;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.Objects;

public class LabeledSeparator extends JSeparator {
    private String label;

    public LabeledSeparator(@Nullable String label) {
        this(label, HORIZONTAL);
    }

    public LabeledSeparator(@Nullable String label, int orientation) {
        super(orientation);

        setLabel(label);
        setUI(LabeledSeparatorUI.createUI(this));
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public void setLabel(@Nullable String label) {
        final String oldLabel = this.label;

        if (!Objects.equals(oldLabel, label)) {
            this.label = label;

            firePropertyChange("label", oldLabel, label);
            revalidate();
            repaint();
        }
    }

    @Override
    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be HORIZONTAL");
        }

        super.setOrientation(orientation);
    }
}
