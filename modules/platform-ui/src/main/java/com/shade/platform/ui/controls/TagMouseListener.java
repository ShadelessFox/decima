package com.shade.platform.ui.controls;

import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class TagMouseListener<T extends ColoredComponent.Tag> extends MouseAdapter {
    public void installOn(@NotNull Component component) {
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            final T tag = getTagAt(e);
            if (tag != null) {
                tag.run(e);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        final Component component = (Component) e.getSource();
        final T tag = getTagAt(e);
        UIUtils.setCursor(component, tag != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
    }

    @Nullable
    protected abstract T getTagAt(@NotNull MouseEvent e);
}
