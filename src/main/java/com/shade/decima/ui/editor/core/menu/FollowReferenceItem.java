package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.editor.NodeEditorInputSimple;
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

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Follow Reference", keystroke = "F4", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 1000)
public class FollowReferenceItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final RTTIReference reference = (RTTIReference) ((CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY)).getValue();

        findNode(new VoidProgressMonitor(), reference, (NodeEditorInput) editor.getInput()).whenComplete((node, exception) -> {
            if (exception != null) {
                UIUtils.showErrorDialog(Application.getFrame(), exception);
                return;
            }

            if (Application.getEditorManager().openEditor(new NodeEditorInputSimple(node), true) instanceof CoreEditor pe) {
                final RTTIObject uuid;

                if (reference instanceof RTTIReference.External ref) {
                    uuid = ref.uuid();
                } else if (reference instanceof RTTIReference.Internal ref) {
                    uuid = ref.uuid();
                } else {
                    return;
                }

                pe.setSelectionPath(new RTTIPath(new RTTIPathElement.UUID(GGUUIDValueHandler.toString(uuid))));
            }
        });
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject node
            && node.getValue() instanceof RTTIReference reference
            && !(reference instanceof RTTIReference.None);
    }

    @NotNull
    private CompletableFuture<NavigatorFileNode> findNode(@NotNull ProgressMonitor monitor, @NotNull RTTIReference reference, @NotNull NodeEditorInput input) {
        if (!(reference instanceof RTTIReference.External ref)) {
            return CompletableFuture.completedFuture(input.getNode());
        }

        final Project project = input.getNode().getProject();
        final Packfile packfile = project.getPackfileManager().findFirst(ref.path());

        if (packfile == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unable to find referenced file"));
        }

        return Application.getNavigator().getModel().findFileNode(
            monitor,
            project.getContainer(),
            packfile,
            PackfileBase.getNormalizedPath(ref.path()).split("/")
        );
    }
}
