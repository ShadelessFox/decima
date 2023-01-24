package com.shade.platform.ui.editors;

import com.shade.util.NotNull;

import java.util.Map;

public interface StatefulEditor extends Editor {
    void loadState(@NotNull Map<String, Object> state);

    void saveState(@NotNull Map<String, Object> state);
}
