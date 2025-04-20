package com.shade.platform.ui.menus.impl;

import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.Service;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.menus.Menu;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.*;
import com.shade.platform.ui.util.EmptyAction;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

@Service(MenuManager.class)
public class MenuManagerImpl implements MenuManager {
    private final List<LazyWithMetadata<Menu, MenuRegistration>> contributedMenus;
    private final List<LazyWithMetadata<MenuItem, MenuItemRegistration>> contributedItems;
    private Map<String, List<LazyWithMetadata<Menu, MenuRegistration>>> menus;
    private Map<String, List<MenuItemGroup>> groups;

    public MenuManagerImpl() {
        this.contributedMenus = ExtensionRegistry.getExtensions(Menu.class, MenuRegistration.class);
        this.contributedItems = ExtensionRegistry.getExtensions(MenuItem.class, MenuItemRegistration.class);

        MenuSelectionManager.defaultManager().addChangeListener(e -> {
            final MenuSelectionManager manager = (MenuSelectionManager) e.getSource();
            final MenuElement[] path = manager.getSelectedPath();
            final MenuSelectionListener publisher = MessageBus.getInstance().publisher(SELECTION);

            if (path.length > 0) {
                final MenuElement element = path[path.length - 1];

                if (element.getComponent() instanceof AbstractButton button && button.getAction() instanceof AbstractMenuAction action) {
                    publisher.selectionChanged(action.item, action.metadata, action.context);
                    return;
                }
            }

            publisher.selectionCleared();
        });
    }

    @Override
    public void installContextMenu(@NotNull JComponent pane, @NotNull String id, @NotNull DataContext context) {
        pane.putClientProperty(CONTEXT_KEY, context);
        UIUtils.installContextMenu(pane, createPopupMenu(pane, id, context));
        createMenuKeyBindings(pane, id, context);
    }

    @Override
    public void installMenuBar(@NotNull JRootPane pane, @NotNull String id, @NotNull DataContext context) {
        pane.setJMenuBar(createMenuBar(id, context));
        createMenuBarKeyBindings(pane, id, context);
    }

    @NotNull
    @Override
    public JToolBar createToolBar(@NotNull JComponent component, @NotNull String id, @NotNull DataContext context) {
        initializeMenuItems();

        final var toolBar = new JToolBar();
        final var groups = this.groups.get(id);

        if (groups == null || groups.isEmpty()) {
            return toolBar;
        }

        final MenuItemContext ctx = new MenuItemContext(context, component, null);
        final ToolBarButtonAdapter adapter = new ToolBarButtonAdapter();

        for (MenuItemGroup group : groups) {
            populateToolBarGroup(toolBar, group, ctx, adapter);
        }

        createMenuKeyBindings(component, id, context);

        return toolBar;
    }

    @Override
    public void update(@NotNull JToolBar toolBar) {
        for (Component component : toolBar.getComponents()) {
            if (component instanceof AbstractButton button && button.getAction() instanceof ToolBarAction action) {
                action.update();
            }
        }
    }

    @Nullable
    @Override
    public MenuItemRegistration findItem(@NotNull String id) {
        for (var contribution : contributedItems) {
            if (contribution.metadata().id().equals(id)) {
                return contribution.metadata();
            }
        }

        return null;
    }

    @NotNull
    private JMenuBar createMenuBar(@NotNull String id, @NotNull DataContext context) {
        initializeMenus();

        final var menuBar = new JMenuBar();
        final var contributions = menus.get(id);

        if (contributions == null || contributions.isEmpty()) {
            return menuBar;
        }

        for (var contribution : contributions) {
            final JMenu menu = new JMenu();
            final Mnemonic mnemonic = Mnemonic.extract(contribution.metadata().name());

            if (mnemonic != null) {
                mnemonic.setText(menu);
            } else {
                menu.setText(contribution.metadata().name());
            }

            final JPopupMenu popupMenu = menu.getPopupMenu();
            popupMenu.addPopupMenuListener(new MyPopupMenuListener(null, popupMenu, contribution.metadata().id(), context));

            menuBar.add(menu);
        }

        return menuBar;
    }

    @NotNull
    @Override
    public JPopupMenu createPopupMenu(@NotNull JComponent component, @NotNull String id, @NotNull DataContext context) {
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.addPopupMenuListener(new MyPopupMenuListener(component, popupMenu, id, context));
        return popupMenu;
    }

    private void populateToolBarGroup(
        @NotNull JToolBar toolBar,
        @NotNull MenuItemProvider provider,
        @NotNull MenuItemContext context,
        @NotNull ToolBarButtonAdapter adapter
    ) {
        final var contributions = provider.create(context).stream()
            .filter(contribution -> contribution.get().isVisible(context))
            .toList();

        if (contributions.isEmpty()) {
            return;
        }

        if (toolBar.getComponentCount() > 0) {
            toolBar.addSeparator();
        }

        for (var contribution : contributions) {
            final MenuItem item = contribution.get();

            if (item instanceof MenuItemProvider p) {
                populateToolBarGroup(toolBar, p, context, adapter);
            } else {
                toolBar.add(createToolBarItem(new ToolBarAction(item, contribution.metadata(), context), adapter));
            }
        }
    }

    @NotNull
    private static JComponent createToolBarItem(@NotNull ToolBarAction action, @NotNull ToolBarButtonAdapter adapter) {
        final AbstractButton button;

        if (action.item instanceof MenuItem.Check) {
            button = new JToggleButton(action);
        } else if (action.item instanceof MenuItem.Radio) {
            button = new JRadioButton(action);
        } else {
            button = new JButton(action);
        }

        button.addMouseListener(adapter);

        return button;
    }

    private void createMenuBarKeyBindings(@NotNull JComponent target, @NotNull String id, @NotNull DataContext context) {
        initializeMenus();

        final var menus = this.menus.get(id);

        if (menus == null || menus.isEmpty()) {
            return;
        }

        for (var menu : menus) {
            createMenuKeyBindings(target, menu.metadata().id(), context);
        }
    }

    private void createMenuKeyBindings(@NotNull JComponent target, @NotNull String id, @NotNull DataContext context) {
        initializeMenuItems();

        final List<MenuItemGroup> groups = this.groups.get(id);

        if (groups == null || groups.isEmpty()) {
            return;
        }

        final MenuItemContext ctx = new MenuItemContext(context, target, null);

        for (MenuItemGroup group : groups) {
            createMenuGroupKeyBindings(target, group, ctx);
        }
    }

    private void createMenuGroupKeyBindings(@NotNull JComponent target, @NotNull MenuItemProvider provider, @NotNull MenuItemContext context) {
        for (var contribution : provider.create(context)) {
            final MenuItem item = contribution.get();

            if (!contribution.metadata().id().isEmpty()) {
                createMenuKeyBindings(target, contribution.metadata().id(), context);
            }

            if (!contribution.metadata().keystroke().isEmpty()) {
                final boolean checkFocus = target instanceof JTree;
                UIUtils.putAction(target, checkFocus ? JComponent.WHEN_FOCUSED : JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke(contribution.metadata().keystroke()), new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (item.isEnabled(context)) {
                            item.perform(context.withEvent(e));
                        }
                    }
                });
            }

            if (item instanceof MenuItemProvider p && !p.isInitializedOnDemand()) {
                createMenuGroupKeyBindings(target, p, context);
            }
        }
    }

    @NotNull
    private JMenuItem createMenuItem(@NotNull MenuItemAction action) {
        if (groups.containsKey(action.metadata.id())) {
            final JMenu menu = new JMenu(action);

            final JPopupMenu popupMenu = menu.getPopupMenu();
            popupMenu.addPopupMenuListener(new MyPopupMenuListener(null, popupMenu, action.metadata.id(), action.context));

            return menu;
        } else if (action.item instanceof MenuItem.Check) {
            return new JCheckBoxMenuItem(action);
        } else if (action.item instanceof MenuItem.Radio) {
            return new JRadioButtonMenuItem(action);
        } else {
            return new JMenuItem(action);
        }
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
            menu.add(new EmptyAction("Nothing"));
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
                menu.add(createMenuItem(new MenuItemAction(item, contribution.metadata(), context)));
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

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            populateMenu(popupMenu, menuId, new MenuItemContext(context, source, null));
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

    private record MenuItemGroup(@NotNull Metadata metadata, @NotNull List<LazyWithMetadata<MenuItem, MenuItemRegistration>> items) implements MenuItemProvider {
        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            return items;
        }

        private record Metadata(@NotNull String id, int order) {
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

    private static abstract class AbstractMenuAction extends AbstractAction {
        protected final MenuItem item;
        protected final MenuItemRegistration metadata;
        protected final MenuItemContext context;

        public AbstractMenuAction(@NotNull MenuItem item, @NotNull MenuItemRegistration metadata, @NotNull MenuItemContext context) {
            this.item = item;
            this.metadata = metadata;
            this.context = context;

            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (item.isEnabled(context)) {
                perform(e);
            }
        }

        public void update() {
            final String name = computeName();
            final Mnemonic mnemonic = Mnemonic.extract(name);

            if (mnemonic != null) {
                putValue(getNameKey(), mnemonic.text());
                putValue(MNEMONIC_KEY, mnemonic.key());
                putValue(DISPLAYED_MNEMONIC_INDEX_KEY, mnemonic.index());
            } else {
                putValue(getNameKey(), name);
            }

            if (item instanceof MenuItem.Check check) {
                putValue(SELECTED_KEY, check.isChecked(context));
            } else if (item instanceof MenuItem.Radio radio) {
                putValue(SELECTED_KEY, radio.isSelected(context));
            }

            putValue(SMALL_ICON, computeIcon());
            putValue(ACCELERATOR_KEY, computeAccelerator());
            putValue(LONG_DESCRIPTION, computeDescription());
            setEnabled(item.isEnabled(context));
        }

        protected abstract void perform(@NotNull ActionEvent e);

        @NotNull
        protected String computeName() {
            return Objects.requireNonNullElseGet(item.getName(context), metadata::name);
        }

        @Nullable
        protected String computeDescription() {
            return metadata.description().isEmpty() ? null : metadata.description();
        }

        @Nullable
        protected Icon computeIcon() {
            final Icon icon = item.getIcon(context);
            if (icon != null) {
                return icon;
            }
            if (metadata.icon().isEmpty()) {
                return null;
            }
            return UIManager.getIcon(metadata.icon());
        }

        @Nullable
        protected KeyStroke computeAccelerator() {
            if (metadata.keystroke().isEmpty()) {
                return null;
            }
            return KeyStroke.getKeyStroke(metadata.keystroke());
        }

        @NotNull
        protected String getNameKey() {
            return NAME;
        }
    }

    private static class MenuItemAction extends AbstractMenuAction {
        public MenuItemAction(@NotNull MenuItem item, @NotNull MenuItemRegistration metadata, @NotNull MenuItemContext context) {
            super(item, metadata, context);
        }

        @Override
        protected void perform(@NotNull ActionEvent e) {
            item.perform(context.withEvent(e));
        }
    }

    private class ToolBarAction extends AbstractMenuAction {
        public ToolBarAction(@NotNull MenuItem item, @NotNull MenuItemRegistration metadata, @NotNull MenuItemContext context) {
            super(item, metadata, context);
        }

        @Override
        public void perform(@NotNull ActionEvent e) {
            if (groups.containsKey(metadata.id())) {
                final JComponent button = (JComponent) e.getSource();
                final JPopupMenu popupMenu = new JPopupMenu();
                popupMenu.addPopupMenuListener(new MyPopupMenuListener(null, popupMenu, metadata.id(), context));
                popupMenu.show(button, 0, button.getHeight());
            } else {
                item.perform(context.withEvent(e));
            }
        }

        @NotNull
        @Override
        protected String computeName() {
            final String name = super.computeName();
            final KeyStroke accelerator = computeAccelerator();

            if (accelerator != null) {
                return "%s (%s)".formatted(name, UIUtils.getTextForAccelerator(accelerator));
            } else {
                return name;
            }
        }

        @NotNull
        @Override
        protected String getNameKey() {
            return SHORT_DESCRIPTION;
        }
    }

    private static class ToolBarButtonAdapter extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            final AbstractButton button = (AbstractButton) e.getSource();

            if (button.getAction() instanceof AbstractMenuAction action) {
                MessageBus.getInstance().publisher(SELECTION).selectionChanged(action.item, action.metadata, action.context);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            MessageBus.getInstance().publisher(SELECTION).selectionCleared();
        }
    }
}
