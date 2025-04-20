package com.shade.platform.ui.controls;

import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;

public class ToolTabbedPane extends JTabbedPane {
    private static final String LAST_DIVIDER_LOCATION_PROPERTY = "lastDividerLocation";

    public ToolTabbedPane(int tabPlacement, @NotNull JSplitPane parent) {
        super(tabPlacement);

        // FIXME: Please replace this faulty implementation with something else
        getModel().addChangeListener(ev -> {
            final int index = getSelectedIndex();
            final Object lastDividerLocation = parent.getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY);

            if (index < 0 && lastDividerLocation == null) {
                parent.putClientProperty(LAST_DIVIDER_LOCATION_PROPERTY, parent.getDividerLocation());
                parent.setDividerLocation(getHeaderSize());
                parent.setDividerSize(1);
            } else if (index >= 0 && lastDividerLocation != null) {
                parent.setDividerLocation((Integer) lastDividerLocation);
                parent.putClientProperty(LAST_DIVIDER_LOCATION_PROPERTY, null);
                parent.setDividerSize(UIManager.getInt("SplitPane.dividerSize"));
            }
        });

        parent.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> {
            if (getSelectedIndex() < 0) {
                parent.setDividerLocation(computeMinimizedDividerLocation(parent));
            }
        });
    }

    @Override
    public String getUIClassID() {
        return "ToolTabbedPaneUI";
    }

    public int getPaneSize() {
        final JSplitPane pane = (JSplitPane) getParent();
        final Object lastDividerLocation = pane.getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY);
        return Objects.requireNonNullElseGet((Integer) lastDividerLocation, pane::getDividerLocation);
    }

    public boolean isPaneMinimized() {
        return ((JSplitPane) getParent()).getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY) != null;
    }

    public void setPaneSize(int size) {
        final JSplitPane pane = (JSplitPane) getParent();

        if (pane.getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY) != null) {
            pane.putClientProperty(LAST_DIVIDER_LOCATION_PROPERTY, size);
        } else {
            pane.setDividerLocation(size);
        }
    }

    public void minimizePane() {
        setSelectedIndex(-1);
        validate();
        repaint();
    }

    private int computeMinimizedDividerLocation(@NotNull JSplitPane pane) {
        return switch (tabPlacement) {
            case LEFT, TOP -> getHeaderSize();
            case RIGHT -> pane.getWidth() - getHeaderSize();
            case BOTTOM -> pane.getHeight() - getHeaderSize();
            default -> throw new IllegalArgumentException("Unexpected tab placement: " + tabPlacement);
        };
    }

    private static int getHeaderSize() {
        return UIManager.getInt("TabbedPane.tabHeight");
    }
}
