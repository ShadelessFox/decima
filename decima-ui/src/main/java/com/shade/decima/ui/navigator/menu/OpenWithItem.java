package com.shade.decima.ui.navigator.menu;

import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.editor.NodeEditorInputSimple;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.EditorProvider;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemProvider;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, id = CTX_MENU_NAVIGATOR_OPEN_ID, name = "&Open with", group = CTX_MENU_NAVIGATOR_GROUP_OPEN, order = 1000)
public class OpenWithItem extends MenuItem {
    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        if (ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode node) {
            final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);
            final NodeEditorInput input = new NodeEditorInputSimple(node);

            return manager.findEditor(input) == null;
        }

        return false;
    }

    @MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_OPEN_ID, name = "&Open with", group = CTX_MENU_NAVIGATOR_OPEN_GROUP_GENERAL, order = 1000)
    public static class PlaceholderItem extends MenuItem implements MenuItemProvider {
        private static final MenuItemRegistration REGISTRATION = MenuItemProvider.createRegistration(CTX_MENU_NAVIGATOR_OPEN_ID, CTX_MENU_NAVIGATOR_OPEN_GROUP_GENERAL);

        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            final NavigatorFileNode node = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
            final NodeEditorInput input = new NodeEditorInputSimple(node);
            final List<LazyWithMetadata<MenuItem, MenuItemRegistration>> items = new ArrayList<>();

            for (EditorProvider provider : ServiceLoader.load(EditorProvider.class)) {
                if (provider.matches(input) != EditorProvider.Match.NONE) {
                    final int index = items.size();
                    final Supplier<MenuItem> item = () -> new MenuItem() {
                        @Override
                        public void perform(@NotNull MenuItemContext ctx) {
                            ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY).openEditor(input, provider, null, true, true);
                        }

                        @Override
                        public String getName(@NotNull MenuItemContext ctx) {
                            return UIUtils.getLabelWithIndexMnemonic(provider.getName(), index);
                        }

                        @Override
                        public Icon getIcon(@NotNull MenuItemContext ctx) {
                            return provider.getIcon();
                        }
                    };

                    items.add(LazyWithMetadata.of(item, REGISTRATION, MenuItem.class));
                }
            }

            return items;
        }

        @Override
        public boolean isInitializedOnDemand() {
            return true;
        }
    }
}
