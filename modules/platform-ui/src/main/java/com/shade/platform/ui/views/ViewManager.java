package com.shade.platform.ui.views;

import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.List;

public interface ViewManager {
    static ViewManager getInstance() {
        return ApplicationManager.getApplication().getService(ViewManager.class);
    }

    @NotNull
    List<LazyWithMetadata<View, ViewRegistration>> getViews();

    @Nullable
    <T extends View> T findView(@NotNull String id);

    boolean isShowing(@NotNull String id, boolean focusRequired);

    void showView(@NotNull String id);

    void hideView(@NotNull String id);

    @NotNull
    JComponent getComponent();
}
