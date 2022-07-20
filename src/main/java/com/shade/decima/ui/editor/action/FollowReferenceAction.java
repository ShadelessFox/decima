package com.shade.decima.ui.editor.action;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.CompletableFuture;

@ActionRegistration(name = "Follow Reference", accelerator = "F4")
@ActionContribution(path = "popup:properties")
public class FollowReferenceAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent event) {
        final EditorManager manager = Application.getFrame().getEditorManager();
        final Editor editor = manager.getActiveEditor();

        if (editor != null && editor.getController().getSelectedValue() instanceof RTTIReference reference) {
            final CompletableFuture<NavigatorFileNode> future = findNode(new VoidProgressMonitor(), reference, editor);

            if (future != null) {
                future.whenComplete((node, exception) -> {
                    if (exception != null) {
                        throw new RuntimeException(exception);
                    }

                    manager
                        .openEditor(new NodeEditorInput(node), true)
                        .getController().setSelectedValue(reference.getUuid());
                });
            }
        }
    }

    @Nullable
    private CompletableFuture<NavigatorFileNode> findNode(@NotNull ProgressMonitor monitor, @NotNull RTTIReference reference, @NotNull Editor editor) {
        if (reference.getUuid() == null) {
            return null;
        }

        if (reference.getPath() == null) {
            return CompletableFuture.completedFuture(editor.getInput().getNode());
        }

        final Project project = UIUtils.getProject(editor.getInput().getNode());
        final Packfile packfile = project.getPackfileManager().findAny(reference.getPath());

        if (packfile != null) {
            final String[] path = PackfileBase.getNormalizedPath(reference.getPath()).split("/");
            return Application.getFrame().getNavigator().findFileNode(monitor, project.getContainer(), packfile, path);
        }

        return CompletableFuture.failedFuture(new IllegalStateException("Unable to find referenced node"));
    }
}
