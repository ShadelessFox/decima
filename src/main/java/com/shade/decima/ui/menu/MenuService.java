package com.shade.decima.ui.menu;

import com.shade.decima.model.app.DataContext;
import com.shade.decima.model.util.LazyWithMetadata;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.model.util.ReflectionUtils;
import com.shade.decima.ui.UIUtils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import java.util.*;

public class MenuService {
    private final List<LazyWithMetadata<Menu, MenuRegistration>> contributedMenus;
    private final List<LazyWithMetadata<MenuItem, MenuItemRegistration>> contributedItems;

    private Map<String, List<LazyWithMetadata<Menu, MenuRegistration>>> menus;
    private Map<String, List<MenuItemGroup>> groups;

    public MenuService() {
        this.contributedMenus = ReflectionUtils.findAnnotatedTypes(Menu.class, MenuRegistration.class);
        this.contributedItems = ReflectionUtils.findAnnotatedTypes(MenuItem.class, MenuItemRegistration.class);
    }

    @NotNull
    public JMenuBar createMenuBar(@NotNull String id) {
        initializeMenus();

        final var menuBar = new JMenuBar();
        final var contributions = menus.get(id);

        if (contributions == null || contributions.isEmpty()) {
            return menuBar;
        }

        for (var contribution : contributions) {
            final JMenu menu = new JMenu();
            final UIUtils.Mnemonic mnemonic = UIUtils.extractMnemonic(contribution.metadata().name());

            if (mnemonic != null) {
                mnemonic.setProperties(menu);
            } else {
                menu.setText(contribution.metadata().name());
            }

            final JPopupMenu popupMenu = menu.getPopupMenu();
            popupMenu.addPopupMenuListener(new MyPopupMenuListener(popupMenu, contribution.metadata().id()));

            menuBar.add(menu);
        }

        return menuBar;
    }

    @NotNull
    public JPopupMenu createContextMenu(@NotNull JComponent component, @NotNull String id, @NotNull DataContext context) {
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.addPopupMenuListener(new MyPopupMenuListener(component, popupMenu, id, context));
        return popupMenu;
    }

    public void createMenuKeyBindings(@NotNull JComponent target, @NotNull String id) {
        initializeMenus();

        final var menus = this.menus.get(id);

        if (menus == null || menus.isEmpty()) {
            return;
        }

        for (var menu : menus) {
            createContextMenuKeyBindings(target, menu.metadata().id(), DataContext.EMPTY);
        }
    }

    public void createContextMenuKeyBindings(@NotNull JComponent target, @NotNull String id, @NotNull DataContext context) {
        initializeMenuItems();

        final InputMap im = target.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final ActionMap am = target.getActionMap();

        final List<MenuItemGroup> groups = this.groups.get(id);

        if (groups == null || groups.isEmpty()) {
            return;
        }

        for (MenuItemGroup group : groups) {
            for (LazyWithMetadata<MenuItem, MenuItemRegistration> item : group.items()) {
                if (item.metadata().keystroke().isEmpty()) {
                    continue;
                }

                final String actionId = UUID.randomUUID().toString();
                final KeyStroke keystroke = KeyStroke.getKeyStroke(item.metadata().keystroke());

                im.put(keystroke, actionId);
                am.put(actionId, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final MenuItem menuItem = item.get();
                        final MenuItemContext ctx = new MenuItemContext(context, null);

                        if (menuItem.isEnabled(ctx)) {
                            menuItem.perform(ctx);
                        }
                    }
                });
            }
        }
    }

    @NotNull
    private JMenuItem createMenuItem(@NotNull MenuItem item, @NotNull MenuItemRegistration metadata, @NotNull MenuItemContext context) {
        final JMenuItem menuItem;

        if (groups.containsKey(metadata.id())) {
            final JMenu menu = new JMenu();

            final JPopupMenu popupMenu = menu.getPopupMenu();
            popupMenu.addPopupMenuListener(new MyPopupMenuListener(null, popupMenu, metadata.id(), context));

            menuItem = menu;
        } else if (item.isChecked(context)) {
            menuItem = new JCheckBoxMenuItem(null, true);
        } else {
            menuItem = new JMenuItem();
        }

        final var name = Objects.requireNonNullElseGet(item.getName(context), metadata::name);
        final var icon = Objects.requireNonNullElseGet(item.getIcon(context), metadata::icon);
        final var mnemonic = UIUtils.extractMnemonic(name);

        if (mnemonic != null) {
            mnemonic.setProperties(menuItem);
        } else {
            menuItem.setText(name);
        }

        if (!icon.isEmpty()) {
            menuItem.setIcon(UIManager.getIcon(icon));
        }

        if (!metadata.keystroke().isEmpty()) {
            menuItem.setAccelerator(KeyStroke.getKeyStroke(metadata.keystroke()));
        }

        if (!item.isEnabled(context)) {
            menuItem.setEnabled(false);
        }

        menuItem.addActionListener(e -> {
            if (item.isEnabled(context)) {
                item.perform(context);
            }
        });

        return menuItem;
    }

    private void populateMenu(@NotNull JPopupMenu menu, @NotNull String id, @NotNull MenuItemContext context) {
        initializeMenuItems();

        final List<MenuItemGroup> groups = this.groups.get(id);

        if (groups == null || groups.isEmpty()) {
            return;
        }

        for (MenuItemGroup group : groups) {
            populateMenuGroup(menu, group, context);
        }

        if (menu.getComponentCount() == 0) {
            menu.add(new PlaceholderAction());
        }
    }

    private void populateMenuGroup(@NotNull JPopupMenu menu, @NotNull MenuItemProvider provider, @NotNull MenuItemContext context) {
        final var contributions = provider.create(context).stream()
            .filter(contribution -> contribution.get().isVisible(context))
            .toList();

        if (contributions.isEmpty()) {
            return;
        }

        if (menu.getComponentCount() > 0) {
            menu.addSeparator();
        }

        for (var contribution : contributions) {
            final MenuItem item = contribution.get();

            if (item instanceof MenuItemProvider p) {
                populateMenuGroup(menu, p, context);
            } else {
                menu.add(createMenuItem(item, contribution.metadata(), context));
            }
        }
    }

    private void initializeMenus() {
        if (menus != null) {
            return;
        }

        menus = new HashMap<>(contributedMenus.size());

        for (var contribution : contributedMenus) {
            menus
                .computeIfAbsent(contribution.metadata().parent(), key -> new ArrayList<>())
                .add(contribution);
        }

        for (var menus : menus.values()) {
            final var seen = new HashSet<>();
            final var ordered = menus.stream()
                .filter(menu -> seen.add(menu.metadata().id()))
                .sorted(Comparator.comparingInt(menu -> menu.metadata().order()))
                .toList();

            menus.clear();
            menus.addAll(ordered);
        }
    }

    private void initializeMenuItems() {
        if (groups != null) {
            return;
        }

        final Map<String, Map<String, MenuItemGroup>> groups = new HashMap<>();

        for (var contribution : contributedItems) {
            final var metadata = MenuItemGroup.Metadata.valueOf(contribution.metadata().group());

            groups
                .computeIfAbsent(contribution.metadata().parent(), key -> new HashMap<>())
                .computeIfAbsent(metadata.id(), key -> new MenuItemGroup(metadata, new ArrayList<>()))
                .items().add(contribution);
        }

        this.groups = new HashMap<>(groups.size());

        for (var entry : groups.entrySet()) {
            final var ordered = entry.getValue().values().stream()
                .sorted(Comparator.comparingInt(group -> group.metadata().order()))
                .peek(group -> group.items().sort(Comparator.comparingInt(item -> item.metadata().order())))
                .toList();

            this.groups.put(entry.getKey(), ordered);
        }
    }

    private class MyPopupMenuListener implements PopupMenuListener {
        private final JComponent source;
        private final JPopupMenu popupMenu;
        private final String menuId;
        private final DataContext context;

        public MyPopupMenuListener(@Nullable JComponent source, @NotNull JPopupMenu popupMenu, @NotNull String menuId, @NotNull DataContext context) {
            this.source = source;
            this.popupMenu = popupMenu;
            this.menuId = menuId;
            this.context = context;
        }

        public MyPopupMenuListener(@NotNull JPopupMenu popupMenu, @NotNull String menuId) {
            this(null, popupMenu, menuId, DataContext.EMPTY);
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            populateMenu(popupMenu, menuId, new MenuItemContext(context, source));
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            popupMenu.removeAll();
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            popupMenu.removeAll();
        }
    }

    private static record MenuItemGroup(@NotNull Metadata metadata, @NotNull List<LazyWithMetadata<MenuItem, MenuItemRegistration>> items) implements MenuItemProvider {
        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext context) {
            return items;
        }

        private static record Metadata(@NotNull String id, int order) {
            @NotNull
            public static Metadata valueOf(@NotNull String value) {
                final int separator = value.indexOf(',');

                if (separator < 0) {
                    throw new IllegalArgumentException("Group has no separator: '" + value + "'");
                }

                final var order = Integer.parseInt(value.substring(0, separator));
                final var id = value.substring(separator + 1);

                return new Metadata(id, order);
            }
        }
    }

    private static class PlaceholderAction extends AbstractAction {
        public PlaceholderAction() {
            super("<Empty>");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // do nothing
        }
    }
}
