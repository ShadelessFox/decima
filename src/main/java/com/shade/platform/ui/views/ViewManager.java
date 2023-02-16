package com.shade.platform.ui.views;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface ViewManager {
    @Nullable
    <T extends View> T findView(@NotNull Class<? extends T> cls);

    void showView(@NotNull Class<? extends View> cls);

    void hideView(@NotNull Class<? extends View> cls);
}
