package com.shade.decima.ui.editor.property.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import java.util.concurrent.CompletableFuture;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_PROPERTY_EDITOR_ID, name = "Follow Reference", keystroke = "F4", group = CTX_MENU_PROPERTY_EDITOR_GROUP_GENERAL, order = 1000)
public class FollowReferenceItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Editor editor = ctx.getData(CommonDataKeys.EDITOR_KEY);
        final RTTIReference reference = (RTTIReference) ctx.getData(CommonDataKeys.SELECTION_KEY);

        if (editor == null || reference == null) {
            return;
        }

        findNode(new VoidProgressMonitor(), reference, editor).whenComplete((node, exception) -> {
            if (exception != null) {
                throw new RuntimeException(exception);
            }

            Application.getFrame().getEditorManager()
                .openEditor(new NodeEditorInput(node), true)
                .getController().setSelectedValue(reference.uuid());
        });
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(CommonDataKeys.SELECTION_KEY) instanceof RTTIReference reference && reference.uuid() != null;
    }

    @NotNull
    private CompletableFuture<NavigatorFileNode> findNode(@NotNull ProgressMonitor monitor, @NotNull RTTIReference reference, @NotNull Editor editor) {
        if (reference.path() == null) {
            return CompletableFuture.completedFuture(editor.getInput().getNode());
        }

        final Project project = UIUtils.getProject(editor.getInput().getNode());
        final Packfile packfile = project.getPackfileManager().findAny(reference.path());

        if (packfile != null) {
            final String[] path = PackfileBase.getNormalizedPath(reference.path()).split("/");
            return Application.getFrame().getNavigator().findFileNode(monitor, project.getContainer(), packfile, path);
        }

        return CompletableFuture.failedFuture(new IllegalStateException("Unable to find referenced node"));
    }
}
