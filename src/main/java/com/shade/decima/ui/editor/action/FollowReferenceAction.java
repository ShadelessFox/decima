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
import com.shade.decima.ui.editor.PropertyEditorPane;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "Follow Reference", accelerator = "F4")
@ActionContribution(path = "popup:properties")
public class FollowReferenceAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent event) {
        final PropertyEditorPane editor = Application.getFrame().getEditorsPane().getActiveEditor();

        if (editor != null && editor.getSelectedValue() instanceof RTTIReference reference) {
            final NavigatorFileNode node;

            try {
                node = findNode(new VoidProgressMonitor(), reference, editor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (node != null) {
                Application.getFrame().getEditorsPane().showEditor(node, reference.getUuid(), true);
            }
        }
    }

    @Nullable
    private NavigatorFileNode findNode(@NotNull ProgressMonitor monitor, @NotNull RTTIReference reference, @NotNull PropertyEditorPane editor) throws Exception {
        if (reference.getUuid() == null) {
            return null;
        }

        if (reference.getPath() == null) {
            return editor.getNode();
        }

        final Project project = UIUtils.getProject(editor.getNode());
        final Packfile packfile = project.getPackfileManager().findAny(reference.getPath());

        if (packfile != null) {
            final String[] path = PackfileBase.getNormalizedPath(reference.getPath()).split("/");
            final NavigatorNode node = Application.getFrame().getNavigator().findFileNode(monitor, project, packfile, path);

            if (node instanceof NavigatorFileNode file) {
                return file;
            }
        }

        return null;
    }
}
