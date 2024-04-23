package com.shade.decima.ui.editor.impl.notifications;

import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.editor.NodeEditorInputSimple;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.EditorNotification;
import com.shade.platform.ui.editors.spi.EditorNotificationProvider;
import com.shade.util.NotNull;

import java.util.Collection;
import java.util.List;

public class SupersededByPatchEditorNotificationProvider implements EditorNotificationProvider {
    @NotNull
    @Override
    public Collection<EditorNotification> getNotifications(@NotNull Editor editor) {
        if (!(editor.getInput() instanceof NodeEditorInput input)) {
            return List.of();
        }

        final NavigatorFileNode node = input.getNode();
        final ArchiveFile currentFile = node.getFile();
        final ArchiveFile actualFile = node.getArchive().getManager().getFile(currentFile.getIdentifier());

        if (currentFile.equals(actualFile)) {
            return List.of();
        }

        return List.of(new EditorNotification(
            EditorNotification.Status.WARNING,
            "<html>This file has been superseded by a patch file. The displayed content may be outdated</html>",
            List.of(
                new EditorNotification.Action("Open File", () -> Application.getNavigator().getModel()
                    .findFileNode(new VoidProgressMonitor(), NavigatorPath.of(
                        node.getProject().getContainer(),
                        actualFile.getArchive(),
                        node.getPath()
                    ))
                    .thenApply(n -> EditorManager.getInstance().openEditor(new NodeEditorInputSimple(n), true)))
            )
        ));
    }
}
