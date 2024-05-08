package com.shade.decima.ui.data;

import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import javax.swing.*;

public interface ValueViewer {
    @NotNull
    JComponent createComponent();

    /**
     * Updates the viewer with the given {@code controller}.
     * <p>
     * Viewers should respect the following rules:
     * <ul>
     *     <li>If the viewer needs to update the UI, it should do so on the EDT thread.</li>
     *     <li>If the monitor is canceled, the viewer should <b>not</b> attempt to update the UI as it might be disposed.</li>
     * </ul>
     *
     * @param monitor    the progress monitor to report progress
     * @param component  the component to update
     * @param controller the value controller to use
     */
    void refresh(@NotNull ProgressMonitor monitor, @NotNull JComponent component, @NotNull ValueController<?> controller);

    default boolean canView(@NotNull ValueController<?> controller) {
        return true;
    }
}
