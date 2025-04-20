package com.shade.platform.ui.wm;

import com.shade.util.Nullable;

public interface StatusBarInfo {
    @Nullable
    String getInfo();

    void setInfo(@Nullable String text);
}
