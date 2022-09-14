package com.shade.decima.ui.controls.plaf;

import com.formdev.flatlaf.ui.FlatComboBoxUI;

import javax.swing.*;
import java.awt.*;

public class ThinFlatComboBoxUI extends FlatComboBoxUI {
    @Override
    protected void installDefaults() {
        final Insets oldInsets = UIManager.getInsets("ComboBox.padding");
        final Insets newInsets = new Insets(0, 0, 0, 0);

        try {
            UIManager.put("ComboBox.padding", newInsets);
            super.installDefaults();
        } finally {
            UIManager.put("ComboBox.padding", oldInsets);
        }
    }
}
