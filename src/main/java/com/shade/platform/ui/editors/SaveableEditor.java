package com.shade.platform.ui.editors;

import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.commands.CommandManager;
import com.shade.util.NotNull;

import java.beans.PropertyChangeListener;

public interface SaveableEditor extends Editor {
    String PROP_DIRTY = "dirty";

    boolean isDirty();

    void doSave(@NotNull ProgressMonitor monitor);

    void addPropertyChangeListener(@NotNull PropertyChangeListener listener);

    void removePropertyChangeListener(@NotNull PropertyChangeListener listener);

    @NotNull
    CommandManager getCommandManager();
}
