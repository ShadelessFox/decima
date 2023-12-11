package com.shade.platform.ui.controls;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ToolTabbedPane extends JTabbedPane {
    private static final String LAST_DIVIDER_LOCATION_PROPERTY = "lastDividerLocation";
    private static final int TAB_HEADER_SIZE = 24;

    public ToolTabbedPane(int tabPlacement) {
        super(tabPlacement);

        addHierarchyListener(e -> {
            if (e.getChangedParent() instanceof JSplitPane splitPane) {
                getModel().addChangeListener(ev -> {
                    final int index = getSelectedIndex();
                    final Object lastDividerLocation = splitPane.getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY);

                    if (index < 0 && lastDividerLocation == null) {
                        splitPane.putClientProperty(LAST_DIVIDER_LOCATION_PROPERTY, splitPane.getDividerLocation());
                        splitPane.setDividerLocation(TAB_HEADER_SIZE);
                    } else if (index >= 0 && lastDividerLocation != null) {
                        splitPane.setDividerLocation((Integer) lastDividerLocation);
                        splitPane.putClientProperty(LAST_DIVIDER_LOCATION_PROPERTY, null);
                    }
                });

                splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> {
                    if (getSelectedIndex() < 0) {
                        splitPane.setDividerLocation(TAB_HEADER_SIZE);
                    }
                });
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

    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);

        if (tabPlacement == LEFT || tabPlacement == RIGHT) {
            final VerticalLabel label = new VerticalLabel(title);
            label.setClockwise(tabPlacement == RIGHT);
            label.setIcon(icon);
            setTabComponentAt(index, label);
        }
    }
}
