package com.shade.platform.ui.editors.spi;

import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorNotification;
import com.shade.util.NotNull;

import java.util.Collection;

public interface EditorNotificationProvider {
    @NotNull
    Collection<EditorNotification> getNotifications(@NotNull Editor editor);
}
