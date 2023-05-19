package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.NodeEditorInputSimple;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.*;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Follow Reference", keystroke = "F4", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 1000)
public class FollowReferenceItem extends MenuItem {
    private static final String POPUP_ID = CTX_MENU_CORE_EDITOR_ID + ".popup";
    private static final String POPUP_GROUP = "1000," + CTX_MENU_CORE_EDITOR_ID + ".general";

    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final Object value = ((CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY)).getValue();

        if (value instanceof RTTIReference reference) {
            follow(reference, editor);
        } else {
            final Tree tree = editor.getTree();
            final Rectangle bounds = Objects.requireNonNull(tree.getPathBounds(tree.getSelectionPath()));
            final JPopupMenu popup = MenuManager.getInstance().createPopupMenu(tree, POPUP_ID, ctx);
            popup.show(tree, bounds.x, bounds.y + bounds.height);
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        if (ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject node) {
            final Object value = node.getValue();

            if (value instanceof RTTIObject object) {
                for (RTTIClass.Field<?> field : object.type().getFields()) {
                    if (isValidReference(field.get(object))) {
                        return true;
                    }
                }
            }

            return isValidReference(value);
        }

        return false;
    }

    private static boolean isValidReference(@Nullable Object value) {
        return value instanceof RTTIReference.Internal
            || value instanceof RTTIReference.External;
    }

    private static void follow(@NotNull RTTIReference reference, @NotNull CoreEditor currentEditor) {
        final RTTIObject uuid;
        final CompletableFuture<Editor> future;

        if (reference instanceof RTTIReference.Internal ref) {
            uuid = ref.uuid();
            future = CompletableFuture.completedFuture(currentEditor);
        } else if (reference instanceof RTTIReference.External ref) {
            uuid = ref.uuid();
            future = findFileNode(new VoidProgressMonitor(), ref, currentEditor.getInput().getProject())
                .thenApply(node -> Application.getEditorManager().openEditor(new NodeEditorInputSimple(node), true));
        } else {
            throw new IllegalStateException("Invalid reference");
        }

        future.whenComplete((editor, exception) -> {
            if (exception != null) {
                UIUtils.showErrorDialog(Application.getInstance().getFrame(), exception);
                return;
            }

            if (editor instanceof CoreEditor e) {
                e.setSelectionPath(new RTTIPath(new RTTIPathElement.UUID(RTTIUtils.uuidToString(uuid))));
            }
        });
    }

    @NotNull
    private static CompletableFuture<NavigatorFileNode> findFileNode(@NotNull ProgressMonitor monitor, @NotNull RTTIReference.External reference, @NotNull Project project) {
        final Packfile packfile = project.getPackfileManager().findFirst(reference.path());

        if (packfile == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unable to find referenced file"));
        }

        return Application.getNavigator().getModel().findFileNode(
            monitor,
            project.getContainer(),
            packfile,
            PackfileBase.getNormalizedPath(reference.path()).split("/")
        );
    }

    @MenuItemRegistration(parent = POPUP_ID, group = POPUP_GROUP, order = 1000)
    public static class PopupItem extends MenuItem implements MenuItemProvider {
        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            final List<LazyWithMetadata<MenuItem, MenuItemRegistration>> items = new ArrayList<>();

            final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
            final CoreNodeObject node = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);
            final RTTIObject object = (RTTIObject) node.getValue();

            for (RTTIClass.Field<?> field : object.type().getFields()) {
                if (field.get(object) instanceof RTTIReference ref && !(ref instanceof RTTIReference.None)) {
                    final int index = items.size();

                    items.add(LazyWithMetadata.of(
                        () -> new ReferenceItem(editor, object, field, index),
                        MenuItemProvider.createRegistration(POPUP_ID, POPUP_GROUP),
                        ReferenceItem.class
                    ));
                }
            }

            items.add(0, LazyWithMetadata.of(
                HeaderItem::new,
                MenuItemProvider.createRegistration(POPUP_ID, POPUP_GROUP),
                HeaderItem.class
            ));

            return items;
        }

        @Override
        public boolean isInitializedOnDemand() {
            return true;
        }
    }

    private static class ReferenceItem extends MenuItem {
        private final CoreEditor editor;
        private final RTTIObject object;
        private final RTTIClass.Field<?> field;
        private final int index;

        public ReferenceItem(@NotNull CoreEditor editor, @NotNull RTTIObject object, @NotNull RTTIClass.Field<?> field, int index) {
            this.editor = editor;
            this.object = object;
            this.field = field;
            this.index = index;
        }

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            follow(object.get(field), editor);
        }

        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            return UIUtils.getLabelWithIndexMnemonic(field.getName(), index);
        }
    }

    private static class HeaderItem extends MenuItem {
        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            return "Follow reference to:";
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            return false;
        }
    }
}
