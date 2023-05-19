package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeBinary;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemProvider;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, id = CTX_MENU_CORE_EDITOR_DECORATION_ID, name = "&Decoration", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 2000)
public class ChangeDecorationItem extends MenuItem {
    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        if (ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject node) {
            final var game = node.getParentOfType(CoreNodeBinary.class).getGameType();
            final var handlers = ValueRegistry.getInstance().findHandlers(node.getValue(), node.getType(), game);
            return handlers.size() > 1;
        }

        return false;
    }

    @MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_DECORATION_ID, group = CTX_MENU_CORE_EDITOR_DECORATION_GROUP_GENERAL, order = 1000)
    public static class PlaceholderItem extends MenuItem implements MenuItemProvider {
        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            final var node = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);
            final var game = node.getParentOfType(CoreNodeBinary.class).getGameType();
            final var handlers = ValueRegistry.getInstance().findHandlers(node.getValue(), node.getType(), game);
            final var registration = MenuItemProvider.createRegistration(CTX_MENU_CORE_EDITOR_DECORATION_ID, CTX_MENU_CORE_EDITOR_DECORATION_GROUP_GENERAL);
            final List<LazyWithMetadata<MenuItem, MenuItemRegistration>> items = new ArrayList<>();

            for (LazyWithMetadata<ValueHandler, ValueHandlerRegistration> handler : handlers) {
                final int index = items.size();
                items.add(LazyWithMetadata.of(() -> new DecorationItem(node, handler, index), registration, DecorationItem.class));
            }

            return items;
        }

        @Override
        public boolean isInitializedOnDemand() {
            return true;
        }
    }

    private static class DecorationItem extends MenuItem implements MenuItem.Radio {
        private final CoreNodeObject node;
        private final LazyWithMetadata<ValueHandler, ValueHandlerRegistration> decoration;
        private final int index;

        public DecorationItem(@NotNull CoreNodeObject node, @NotNull LazyWithMetadata<ValueHandler, ValueHandlerRegistration> decoration, int index) {
            this.node = node;
            this.decoration = decoration;
            this.index = index;
        }

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
            node.setHandler(decoration.get());
            editor.getTree().getModel().fireStructureChanged(node);
        }

        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            return UIUtils.getLabelWithIndexMnemonic(decoration.metadata().name(), index);
        }

        @Override
        public boolean isSelected(@NotNull MenuItemContext ctx) {
            final ValueHandlerRegistration metadata = node.getHandler().getClass().getDeclaredAnnotation(ValueHandlerRegistration.class);
            return metadata.id().equals(decoration.metadata().id());
        }
    }
}
