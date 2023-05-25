package com.shade.decima.ui.views;

import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.Service;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.platform.ui.controls.ToolTabbedPane;
import com.shade.platform.ui.controls.plaf.ThinFlatSplitPaneUI;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.views.View;
import com.shade.platform.ui.views.ViewManager;
import com.shade.platform.ui.views.ViewRegistration;
import com.shade.platform.ui.views.ViewRegistration.Anchor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service(ViewManager.class)
@Persistent("ViewManager")
public class ViewManagerImpl implements ViewManager, PersistableComponent<ViewManagerImpl.State[]> {
    private static final DataKey<View> VIEW_KEY = new DataKey<>("view", View.class);
    private static final DataKey<ViewRegistration> VIEW_REGISTRATION_KEY = new DataKey<>("viewRegistration", ViewRegistration.class);

    private JComponent component;

    @NotNull
    @Override
    public List<LazyWithMetadata<View, ViewRegistration>> getViews() {
        return ExtensionRegistry.getExtensions(View.class, ViewRegistration.class);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends View> T findView(@NotNull String id) {
        final JComponent component = findViewComponent(getComponent(), id);

        if (component != null) {
            return (T) VIEW_KEY.get(component);
        } else {
            return null;
        }
    }

    @Override
    public void showView(@NotNull String id) {
        final JComponent component = findViewComponent(getComponent(), id);

        if (component != null) {
            final View view = VIEW_KEY.get(component);
            final ToolTabbedPane pane = (ToolTabbedPane) component.getParent();

            pane.setSelectedComponent(component);
            view.setFocus();
        }
    }

    @Override
    public void hideView(@NotNull String id) {
        final JComponent component = findViewComponent(getComponent(), id);

        if (component != null) {
            final ToolTabbedPane pane = (ToolTabbedPane) component.getParent();

            if (pane.getSelectedComponent() == component) {
                pane.setSelectedIndex(-1);

                final Editor editor = EditorManager.getInstance().getActiveEditor();

                if (editor != null) {
                    editor.setFocus();
                }
            }
        }
    }

    @Override
    public boolean isShowing(@NotNull String id, boolean focusRequired) {
        final JComponent component = findViewComponent(getComponent(), id);

        if (component != null) {
            final ToolTabbedPane pane = (ToolTabbedPane) component.getParent();
            final View view = VIEW_KEY.get(component);
            return !pane.isPaneMinimized() && (!focusRequired || view.isFocused());
        }

        return false;
    }

    @NotNull
    @Override
    public synchronized JComponent getComponent() {
        if (component == null) {
            component = createViewPanels(EditorManager.getInstance().getContainer());
        }

        return component;
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

    @Nullable
    @Override
    public State[] getState() {
        final List<State> states = new ArrayList<>();
        saveViews(states, getComponent());
        return states.toArray(State[]::new);
    }

    @Override
    public void loadState(@NotNull State[] state) {
        restoreViews(state, getComponent());
    }

    private static void saveViews(@NotNull List<State> states, @NotNull Component component) {
        if (component instanceof JSplitPane pane) {
            saveViews(states, pane.getLeftComponent());
            saveViews(states, pane.getRightComponent());
        } else if (component instanceof ToolTabbedPane pane) {
            final JComponent selection = (JComponent) pane.getSelectedComponent();
            states.add(new State(
                Anchor.valueOf(pane.getTabPlacement()),
                selection != null ? VIEW_REGISTRATION_KEY.get(selection).id() : null,
                pane.getPaneSize(),
                pane.isPaneMinimized()
            ));
        }
    }

    private static void restoreViews(@NotNull State[] states, @NotNull Component component) {
        if (component instanceof JSplitPane pane) {
            restoreViews(states, pane.getLeftComponent());
            restoreViews(states, pane.getRightComponent());
        } else if (component instanceof ToolTabbedPane pane) {
            final Anchor anchor = Anchor.valueOf(pane.getTabPlacement());
            final State state = Arrays.stream(states)
                .filter(p -> p.anchor == anchor)
                .findFirst().orElse(null);

            if (state != null) {
                if (state.selection != null) {
                    for (int i = 0; i < pane.getTabCount(); i++) {
                        final JComponent tab = (JComponent) pane.getComponentAt(i);
                        final ViewRegistration registration = VIEW_REGISTRATION_KEY.get(tab);

                        if (registration.id().equals(state.selection)) {
                            pane.setSelectedIndex(i);
                            break;
                        }
                    }
                }

                if (state.size > 0) {
                    pane.setPaneSize(state.size);
                }

                if (state.minimized) {
                    pane.minimizePane();
                }
            }
        }
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

    protected record State(@NotNull Anchor anchor, @Nullable String selection, int size, boolean minimized) {}
}
