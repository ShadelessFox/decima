package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemProvider;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.hash.spi.Hasher;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, id = CopyHashItem.ID, name = "Copy Value &Hash", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 5000)
public class CopyHashItem extends MenuItem {
    public static final String ID = CTX_MENU_CORE_EDITOR_ID + ".copyHash";
    public static final String GROUP = "1000," + ID + ".general";

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject node
            && node.getValue() instanceof String;
    }

    @MenuItemRegistration(parent = ID, group = GROUP, order = 1000)
    public static class HashPlaceholderItem extends MenuItem implements MenuItemProvider {
        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            final List<LazyWithMetadata<MenuItem, MenuItemRegistration>> items = new ArrayList<>();

            for (Hasher provider : ServiceLoader.load(Hasher.class)) {
                final int index = items.size();

                items.add(LazyWithMetadata.of(
                    () -> new HashItem(provider, index),
                    MenuItemProvider.createRegistration(ID, GROUP),
                    HashItem.class
                ));
            }

            return items;
        }
    }

    public static class HashItem extends MenuItem {
        private final Hasher hasher;
        private final int index;

        public HashItem(@NotNull Hasher hasher, int index) {
            this.hasher = hasher;
            this.index = index;
        }

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final CoreNodeObject node = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);
            final String text = (String) node.getValue();
            final String hash;

            if (hasher instanceof Hasher.ToInt h) {
                hash = "%#010x".formatted(h.calculate(text.getBytes()));
            } else if (hasher instanceof Hasher.ToLong h) {
                hash = "%#018x".formatted(h.calculate(text.getBytes()));
            } else {
                throw new NotImplementedException();
            }

            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            final StringSelection selection = new StringSelection(hash);
            clipboard.setContents(selection, selection);
        }

        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            return UIUtils.getLabelWithIndexMnemonic(hasher.name(), index);
        }
    }
}
