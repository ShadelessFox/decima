package com.shade.decima.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shade.decima.ui.controls.MemoryIndicator;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.FileEditorInputLazy;
import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.controls.ToolTabbedPane;
import com.shade.platform.ui.controls.plaf.ThinFlatSplitPaneUI;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.StatefulEditor;
import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.platform.ui.editors.stack.EditorStackContainer;
import com.shade.platform.ui.editors.stack.EditorStackManager;
import com.shade.platform.ui.views.View;
import com.shade.platform.ui.views.ViewManager;
import com.shade.platform.ui.views.ViewRegistration;
import com.shade.platform.ui.views.ViewRegistration.Anchor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ApplicationPane extends JPanel implements ViewManager {
    private static final Logger log = LoggerFactory.getLogger(ApplicationPane.class);
    private static final Gson gson = new Gson();

    private static final DataKey<View> VIEW_KEY = new DataKey<>("view", View.class);
    private static final DataKey<ViewRegistration> VIEW_REGISTRATION_KEY = new DataKey<>("viewRegistration", ViewRegistration.class);

    private final EditorStackManager editorManager;
    private final JComponent root;

    public ApplicationPane() {
        this.editorManager = new EditorStackManager();
        this.root = createViewPanels(editorManager.getContainer());
        this.root.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.shadow")));

        final JToolBar toolbar = new JToolBar();
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(new MemoryIndicator());

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);
        add(toolbar, BorderLayout.SOUTH);
    }

    @NotNull
    @Override
    public List<LazyWithMetadata<View, ViewRegistration>> getViews() {
        return ExtensionRegistry.getExtensions(View.class, ViewRegistration.class);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends View> T findView(@NotNull String id) {
        final JComponent component = findViewComponent(root, id);

        if (component != null) {
            return (T) VIEW_KEY.get(component);
        } else {
            return null;
        }
    }

    @Override
    public void showView(@NotNull String id) {
        final JComponent component = findViewComponent(root, id);

        if (component != null) {
            final View view = VIEW_KEY.get(component);
            final ToolTabbedPane pane = (ToolTabbedPane) component.getParent();

            pane.setSelectedComponent(component);
            view.setFocus();
        }
    }

    @Override
    public void hideView(@NotNull String id) {
        final JComponent component = findViewComponent(root, id);

        if (component != null) {
            final ToolTabbedPane pane = (ToolTabbedPane) component.getParent();

            if (pane.getSelectedComponent() == component) {
                pane.setSelectedIndex(-1);

                final Editor editor = editorManager.getActiveEditor();

                if (editor != null) {
                    editor.setFocus();
                }
            }
        }
    }

    @Override
    public boolean isShowing(@NotNull String id, boolean focusRequired) {
        final JComponent component = findViewComponent(root, id);

        if (component != null) {
            final ToolTabbedPane pane = (ToolTabbedPane) component.getParent();
            final View view = VIEW_KEY.get(component);
            return !pane.isPaneMinimized() && (!focusRequired || view.isFocused());
        }

        return false;
    }

    @NotNull
    public EditorStackManager getEditorManager() {
        return editorManager;
    }

    void saveEditors(@NotNull Preferences pref) {
        saveEditors(pref, editorManager.getContainer());
    }

    void restoreEditors(@NotNull Preferences pref) {
        restoreEditors(pref, editorManager, editorManager.getContainer());
    }

    void saveViews(@NotNull Preferences pref) {
        saveViews(pref, root);
    }

    void restoreViews(@NotNull Preferences pref) throws BackingStoreException {
        restoreViews(pref, root);
    }

    private static void saveEditors(@NotNull Preferences pref, @NotNull Component element) {
        if (element instanceof EditorStackContainer container) {
            pref.put("type", container.isSplit() ? "split" : "stack");

            if (container.isSplit()) {
                pref.put("orientation", container.getSplitOrientation() == JSplitPane.HORIZONTAL_SPLIT ? "horizontal" : "vertical");
                pref.putDouble("position", container.getSplitPosition());
            } else {
                pref.putInt("selection", container.getSelectionIndex());
            }

            final Component[] children = container.getChildren();
            for (int i = 0; i < children.length; i++) {
                saveEditors(pref.node(String.valueOf(i)), children[i]);
            }
        } else {
            final Editor editor = PlatformDataKeys.EDITOR_KEY.get((JComponent) element);
            final String project;
            final String packfile;
            final String resource;

            if (editor.getInput() instanceof FileEditorInputLazy input) {
                project = input.container().toString();
                packfile = input.packfile();
                resource = input.path().full();
            } else if (editor.getInput() instanceof FileEditorInput input) {
                project = input.getProject().getContainer().getId().toString();
                packfile = input.getNode().getPackfile().getPath().getFileName().toString();
                resource = input.getNode().getPath().full();
            } else {
                return;
            }

            pref.put("project", project);
            pref.put("packfile", packfile);
            pref.put("resource", resource);

            if (editor instanceof StatefulEditor se) {
                final Map<String, Object> state = new HashMap<>();

                try {
                    se.saveState(state);
                } catch (Exception e) {
                    log.error("Unable to save state of editor '" + se + "' with input '" + se.getInput() + "'", e);
                    return;
                }

                if (state.isEmpty()) {
                    pref.remove("state");
                } else {
                    pref.put("state", gson.toJson(state));
                }
            }
        }
    }

    private static void restoreEditors(@NotNull Preferences pref, @NotNull EditorStackManager manager, @NotNull EditorStackContainer container) {
        final String type = pref.get("type", "stack");
        final Preferences[] children = IOUtils.children(pref);
        Arrays.sort(children, Comparator.comparingInt(p -> Integer.parseInt(p.name())));

        if (type.equals("split")) {
            final var orientation = pref.get("orientation", "horizontal").equals("horizontal") ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;
            final var position = pref.getDouble("position", 0.5);
            final var result = container.split(orientation, position, false);

            restoreEditors(children[0], manager, result.leading());
            restoreEditors(children[1], manager, result.trailing());
        } else {
            final var selection = pref.getInt("selection", 0);
            final var stack = (EditorStack) container.getComponent(0);

            restoreEditor(children[selection], manager, stack, 0, true);

            for (int i = 0; i < children.length; i++) {
                if (i != selection) {
                    restoreEditor(children[i], manager, stack, i, false);
                }
            }
        }
    }

    private static void restoreEditor(@NotNull Preferences node, @NotNull EditorManager manager, @NotNull EditorStack stack, int index, boolean select) {
        final var project = IOUtils.getNotNull(node, "project");
        final var packfile = IOUtils.getNotNull(node, "packfile");
        final var resource = IOUtils.getNotNull(node, "resource");
        final var input = new FileEditorInputLazy(project, packfile, resource);
        final var editor = manager.openEditor(input, null, stack, select, select, index);

        if (editor instanceof StatefulEditor se) {
            final String state = node.get("state", null);

            if (state != null) {
                try {
                    se.loadState(gson.fromJson(state, new TypeToken<Map<String, Object>>() {}.getType()));
                } catch (Exception e) {
                    log.error("Unable to restore state of editor '" + se + "' with input '" + input + "'", e);
                }
            }
        }
    }

    private static void saveViews(@NotNull Preferences pref, @NotNull Component component) {
        if (component instanceof JSplitPane pane) {
            saveViews(pref, pane.getLeftComponent());
            saveViews(pref, pane.getRightComponent());
        } else if (component instanceof ToolTabbedPane pane) {
            final Preferences node = pref.node(getToolPaneName(pane));
            node.putInt("size", pane.getPaneSize());
            node.putBoolean("minimized", pane.isPaneMinimized());

            final JComponent selection = (JComponent) pane.getSelectedComponent();
            if (selection != null) {
                node.put("selection", VIEW_REGISTRATION_KEY.get(selection).id());
            }
        }
    }

    @NotNull
    private static String getToolPaneName(@NotNull ToolTabbedPane pane) {
        return switch (pane.getTabPlacement()) {
            case SwingConstants.TOP -> "top";
            case SwingConstants.LEFT -> "left";
            case SwingConstants.BOTTOM -> "bottom";
            case SwingConstants.RIGHT -> "right";
            default -> throw new IllegalStateException("Unsupported tab placement: " + pane.getTabPlacement());
        };
    }

    private static void restoreViews(@NotNull Preferences pref, @NotNull Component component) throws BackingStoreException {
        if (component instanceof JSplitPane pane) {
            restoreViews(pref, pane.getLeftComponent());
            restoreViews(pref, pane.getRightComponent());
        } else if (component instanceof ToolTabbedPane pane) {
            final String name = getToolPaneName(pane);
            if (pref.nodeExists(name)) {
                final Preferences node = pref.node(name);
                final var selection = node.get("selection", null);
                final var size = node.getInt("size", 0);
                final var minimized = node.getBoolean("minimized", false);

                if (selection != null) {
                    for (int i = 0; i < pane.getTabCount(); i++) {
                        final JComponent tab = (JComponent) pane.getComponentAt(i);
                        final ViewRegistration registration = VIEW_REGISTRATION_KEY.get(tab);

                        if (registration.id().equals(selection)) {
                            pane.setSelectedIndex(i);
                            break;
                        }
                    }
                }

                if (size > 0) {
                    pane.setPaneSize(size);
                }

                if (minimized) {
                    pane.minimizePane();
                }
            }
        }
    }

    @Nullable
    private JComponent findViewComponent(@NotNull Component component, @NotNull String id) {
        if (component instanceof JSplitPane pane) {
            final JComponent comp = findViewComponent(pane.getLeftComponent(), id);

            if (comp != null) {
                return comp;
            } else {
                return findViewComponent(pane.getRightComponent(), id);
            }
        } else if (component instanceof ToolTabbedPane pane) {
            for (int i = 0; i < pane.getTabCount(); i++) {
                final JComponent tab = (JComponent) pane.getComponentAt(i);
                final ViewRegistration registration = VIEW_REGISTRATION_KEY.get(tab);

                if (registration.id().equals(id)) {
                    return tab;
                }
            }
        }

        return null;
    }

    @NotNull
    private JComponent createViewPanels(@NotNull JComponent root) {
        final var contributions = getViews();

        root = createViewPanel(root, Anchor.LEFT, contributions);
        root = createViewPanel(root, Anchor.RIGHT, contributions);
        root = createViewPanel(root, Anchor.BOTTOM, contributions);

        return root;
    }

    @NotNull
    private JComponent createViewPanel(@NotNull JComponent root, @NotNull Anchor anchor, @NotNull List<LazyWithMetadata<View, ViewRegistration>> contributions) {
        final var views = getViews(anchor, contributions);

        if (views.isEmpty()) {
            return root;
        }

        final ToolTabbedPane tabbedPane = new ToolTabbedPane(anchor.toSwingConstant());

        for (var view : views) {
            final JComponent component = new ViewPane(view.metadata(), view.get().createComponent());
            component.putClientProperty(VIEW_KEY, view.get());
            component.putClientProperty(VIEW_REGISTRATION_KEY, view.metadata());

            tabbedPane.addTab(view.metadata().label(), UIManager.getIcon(view.metadata().icon()), component);
        }

        final JSplitPane splitPane = new JSplitPane();
        splitPane.setUI(new ThinFlatSplitPaneUI());

        switch (anchor) {
            case LEFT -> {
                splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                splitPane.setLeftComponent(tabbedPane);
                splitPane.setRightComponent(root);
            }
            case RIGHT -> {
                splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                splitPane.setLeftComponent(root);
                splitPane.setRightComponent(tabbedPane);
            }
            case BOTTOM -> {
                splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
                splitPane.setLeftComponent(root);
                splitPane.setRightComponent(tabbedPane);
            }
        }

        return splitPane;
    }

    @NotNull
    private static List<LazyWithMetadata<View, ViewRegistration>> getViews(@NotNull Anchor anchor, @NotNull List<LazyWithMetadata<View, ViewRegistration>> contributions) {
        return contributions.stream()
            .filter(c -> c.metadata().anchor() == anchor)
            .sorted(Comparator.comparingInt(c -> c.metadata().order()))
            .toList();
    }

    private class ViewPane extends JComponent {
        public ViewPane(@NotNull ViewRegistration registration, @NotNull Component component) {
            final JToolBar toolbar = new JToolBar();
            toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.shadow")),
                BorderFactory.createEmptyBorder(0, 8, 0, 0)
            ));
            toolbar.add(new JLabel(registration.label() + ": "));
            toolbar.add(Box.createHorizontalGlue());
            toolbar.add(new AbstractAction("Hide", UIManager.getIcon("Toolbar.hideIcon")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    hideView(registration.id());
                }
            });

            setLayout(new BorderLayout());
            add(toolbar, BorderLayout.NORTH);
            add(component, BorderLayout.CENTER);
        }
    }
}
