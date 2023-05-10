package com.shade.decima.ui.controls.hex.impl;

import com.shade.decima.ui.controls.hex.HexCaret;
import com.shade.util.NotNull;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public final class DefaultHexCaret implements HexCaret {
    private final EventListenerList listeners = new EventListenerList();

    private int dot;
    private int mark;

    @Override
    public void addChangeListener(@NotNull ChangeListener listener) {
        listeners.add(ChangeListener.class, listener);
    }

    @Override
    public void removeChangeListener(@NotNull ChangeListener listener) {
        listeners.remove(ChangeListener.class, listener);
    }

    @Override
    public int getDot() {
        return dot;
    }

    @Override
    public int getMark() {
        return mark;
    }

    @Override
    public void setDot(int dot) {
        if (this.dot != dot || this.mark != dot) {
            this.dot = dot;
            this.mark = dot;

            fireCaretChanged();
        }
    }

    @Override
    public void moveDot(int dot) {
        if (this.dot != dot) {
            this.dot = dot;

            fireCaretChanged();
        }
    }

    private void fireCaretChanged() {
        final ChangeEvent event = new ChangeEvent(this);

        for (ChangeListener listener : listeners.getListeners(ChangeListener.class)) {
            listener.stateChanged(event);
        }
    }
}
