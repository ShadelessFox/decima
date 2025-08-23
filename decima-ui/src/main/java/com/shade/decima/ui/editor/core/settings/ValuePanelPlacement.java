package com.shade.decima.ui.editor.core.settings;

public enum ValuePanelPlacement {
    RIGHT("Right"),
    BOTTOM("Bottom");

    private final String label;

    ValuePanelPlacement(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
