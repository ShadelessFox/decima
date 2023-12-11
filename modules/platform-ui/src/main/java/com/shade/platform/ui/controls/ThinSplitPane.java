package com.shade.platform.ui.controls;

import javax.swing.*;

public class ThinSplitPane extends JSplitPane {
    public ThinSplitPane() {
    }

    public ThinSplitPane(int newOrientation) {
        super(newOrientation);
    }

    @Override
    public String getUIClassID() {
        return "ThinSplitPaneUI";
    }
}
