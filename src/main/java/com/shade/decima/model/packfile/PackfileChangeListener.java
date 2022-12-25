package com.shade.decima.model.packfile;

import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.ui.navigator.impl.FilePath;
import com.shade.util.NotNull;

import java.util.EventListener;

public interface PackfileChangeListener extends EventListener {
    void fileChanged(@NotNull Packfile packfile, @NotNull FilePath path, @NotNull Change change);
}
