package com.shade.decima.ui.navigator.menu;

import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.FileEditorInputSimple;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.EditorProvider;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemProvider;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.lang.annotation.Annotation;
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
            final FileEditorInput input = new FileEditorInputSimple(node);

            return manager.findEditor(input) == null;
        }

        return false;
    }

    @MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_OPEN_ID, name = "&Open with", group = CTX_MENU_NAVIGATOR_OPEN_GROUP_GENERAL, order = 1000)
    public static class PlaceholderItem extends MenuItem implements MenuItemProvider {
        private static final MenuItemRegistration REGISTRATION = new MenuItemRegistration() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return MenuItemRegistration.class;
            }

            @Override
            public String parent() {
                return CTX_MENU_NAVIGATOR_OPEN_ID;
            }

            @Override
            public String id() {
                return "";
            }

            @Override
            public String name() {
                return "";
            }

            @Override
            public String icon() {
                return "";
            }

            @Override
            public String keystroke() {
                return "";
            }

            @Override
            public String group() {
                return CTX_MENU_NAVIGATOR_OPEN_GROUP_GENERAL;
            }

            @Override
            public int order() {
                return 1000;
            }
        };

        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            final NavigatorFileNode node = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
            final FileEditorInput input = new FileEditorInputSimple(node);
            final List<LazyWithMetadata<MenuItem, MenuItemRegistration>> items = new ArrayList<>();

            for (EditorProvider provider : ServiceLoader.load(EditorProvider.class)) {
                if (provider.supports(input)) {
                    final int index = items.size() + 1;
                    final Supplier<MenuItem> item = () -> new MenuItem() {
                        @Override
                        public void perform(@NotNull MenuItemContext ctx) {
                            ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY).openEditor(input, provider, null, true, true);
                        }

                        @Override
                        public String getName(@NotNull MenuItemContext ctx) {
                            return "&%d. %s".formatted(index, provider.getName());
                        }

                        @Override
                        public Icon getIcon(@NotNull MenuItemContext ctx) {
                            return provider.getIcon();
                        }
                    };

                    items.add(LazyWithMetadata.of(item, REGISTRATION));
                }
            }

            return items;
        }
    }
}
