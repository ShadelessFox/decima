package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.FileEditorInputSimple;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import java.util.concurrent.CompletableFuture;

import static com.shade.decima.ui.menu.MenuConstants.CTX_MENU_CORE_EDITOR_GROUP_GENERAL;
import static com.shade.decima.ui.menu.MenuConstants.CTX_MENU_CORE_EDITOR_ID;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Follow Reference", keystroke = "F4", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 1000)
public class FollowReferenceItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final RTTIReference reference = (RTTIReference) ((CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY)).getObject();

        findNode(new VoidProgressMonitor(), reference, (FileEditorInput) editor.getInput()).whenComplete((node, exception) -> {
            if (exception != null) {
                UIUtils.showErrorDialog(exception);
                return;
            }

            if (Application.getFrame().getEditorManager().openEditor(new FileEditorInputSimple(node), true) instanceof CoreEditor pe) {
                pe.setSelectedValue(reference.uuid());
            }
        });
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject node
               && node.getObject() instanceof RTTIReference reference
               && reference.uuid() != null;
    }

    @NotNull
    private CompletableFuture<NavigatorFileNode> findNode(@NotNull ProgressMonitor monitor, @NotNull RTTIReference reference, @NotNull FileEditorInput input) {
        if (reference.path() == null) {
            return CompletableFuture.completedFuture(input.getNode());
        }

        final Project project = input.getNode().getProject();
        final Packfile packfile = project.getPackfileManager().findAny(reference.path());

        if (packfile == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unable to find referenced file"));
        }

        return Application.getFrame().getNavigator().getModel()
            .findFileNode(monitor, project.getContainer(), packfile, PackfileBase.getNormalizedPath(reference.path()).split("/"));
    }
}
