package com.shade.platform.ui.util;

import com.shade.util.NotNull;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class HyperlinkAdapter implements HyperlinkListener {
    @Override
    public final void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            hyperlinkActivated(e);
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            hyperlinkEntered(e);
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            hyperlinkExited(e);
        }
    }

    public void hyperlinkActivated(@NotNull HyperlinkEvent e) {
        // do nothing by default
    }

    public void hyperlinkEntered(@NotNull HyperlinkEvent e) {
        // do nothing by default
    }

    public void hyperlinkExited(@NotNull HyperlinkEvent e) {
        // do nothing by default
    }
}
