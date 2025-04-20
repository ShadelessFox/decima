package com.shade.decima.ui.editor.impl.notifications;

import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorNotification;
import com.shade.platform.ui.editors.spi.EditorNotificationProvider;
import com.shade.util.NotNull;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

public class LoadedWithErrorsEditorNotificationProvider implements EditorNotificationProvider {
    @NotNull
    @Override
    public Collection<EditorNotification> getNotifications(@NotNull Editor editor) {
        if (!(editor instanceof CoreEditor coreEditor) || coreEditor.getErrorCount() == 0) {
            return List.of();
        }

        return List.of(new EditorNotification(
            EditorNotification.Status.ERROR,
            MessageFormat.format(
                "<html>This file was loaded with <b>{0}</b> {0,choice,1#error|1<errors}. "
                    + "Refer to the console output for more information</html>",
                coreEditor.getErrorCount()
            ),
            List.of()
        ));
    }
}
