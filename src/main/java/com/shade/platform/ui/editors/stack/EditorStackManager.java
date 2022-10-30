package com.shade.platform.ui.editors.stack;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.editors.*;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.platform.ui.menus.MenuService;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.shade.platform.ui.PlatformDataKeys.EDITOR_KEY;

public class EditorStackManager implements EditorManager, PropertyChangeListener {
    private static final ServiceLoader<EditorProvider> EDITOR_PROVIDERS = ServiceLoader.load(EditorProvider.class);

    private final Map<LazyEditorInput, LoadingWorker> workers = Collections.synchronizedMap(new HashMap<>());
    private final EventListenerList listeners = new EventListenerList();
    private final EditorStackContainer container;

    private EditorStack lastEditorStack;

    public EditorStackManager() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(e -> {
            if ("permanentFocusOwner".equals(e.getPropertyName())) {
                final EditorStack stack = getEditorStack((Component) e.getNewValue());

                if (stack != null) {
                    lastEditorStack = stack;
                    fireEditorChangeEvent(EditorChangeListener::editorChanged, getActiveEditor());
                }
            }
        });

        addEditorChangeListener(new EditorChangeListener() {
            @Override
            public void editorStackCreated(@NotNull EditorStack stack) {
                final DataContext context = key -> switch (key) {
                    case "editor" -> getActiveEditor();
                    case "editorStack" -> stack;
                    case "editorManager" -> EditorStackManager.this;
                    default -> null;
                };

                final MenuService menuService = Application.getMenuService();
                UIUtils.installPopupMenu(stack, menuService.createContextMenu(stack, MenuConstants.CTX_MENU_EDITOR_STACK_ID, context));
                menuService.createContextMenuKeyBindings(stack, MenuConstants.CTX_MENU_EDITOR_STACK_ID, context);

                stack.addChangeListener(e -> {
                    if (stack.getSelectedComponent() instanceof JComponent component) {
                        final Editor editor = EDITOR_KEY.get(component);

                        if (editor.getInput() instanceof LazyEditorInput input) {
                            workers.computeIfAbsent(input, key -> {
                                final LoadingWorker worker = new LoadingWorker(component, key);
                                worker.execute();
                                return worker;
                            });
                        }
                    }
                });
            }
        });

        this.container = new EditorStackContainer(this, null);
    }

    @Nullable
    @Override
    public Editor findEditor(@NotNull EditorInput input) {
        final JComponent component = findEditorComponent(e -> e.getInput().representsSameResource(input));

        if (component != null) {
            return EDITOR_KEY.get(component);
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public Editor openEditor(@NotNull EditorInput input, boolean focus) {
        return openEditor(input, null, null, true, focus);
    }

    @NotNull
    @Override
    public Editor openEditor(@NotNull EditorInput input, @Nullable EditorProvider provider, @Nullable EditorStack stack, boolean select, boolean focus) {
        JComponent component = findEditorComponent(e -> e.getInput().representsSameResource(input));

        if (component == null) {
            if (provider == null) {
                provider = findSuitableProvider(input);
            }

            final Editor editor = provider.createEditor(input);

            if (editor instanceof SaveableEditor se) {
                se.addPropertyChangeListener(this);
            }

            component = editor.createComponent();
            component.putClientProperty(EDITOR_KEY, editor);

            stack = Objects.requireNonNullElseGet(stack, this::getActiveStack);
            stack.addTab(input.getName(), provider.getIcon(), component, input.getDescription());
        } else {
            stack = ((EditorStack) component.getParent());
        }

        final Editor editor = EDITOR_KEY.get(component);

        if (select && stack.getSelectedComponent() != component) {
            // HACK: Prevent focus from being transferred if not required
            stack.setFocusable(false);
            stack.setSelectedComponent(component);
            stack.setFocusable(true);

            fireEditorChangeEvent(EditorChangeListener::editorOpened, editor);
        }

        if (focus) {
            editor.setFocus();
        }

        return editor;
    }

    @NotNull
    private EditorProvider findSuitableProvider(@NotNull EditorInput input) {
        for (EditorProvider provider : EDITOR_PROVIDERS) {
            if (provider.supports(input)) {
                return provider;
            }
        }

        throw new IllegalArgumentException("Unable to find a suitable editor for input: " + input);
    }

    @Nullable
    @Override
    public Editor getActiveEditor() {
        final JComponent component = (JComponent) getActiveStack().getSelectedComponent();

        if (component != null) {
            return EDITOR_KEY.get(component);
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public Editor[] getEditors() {
        final List<Editor> editors = new ArrayList<>();

        forEachStack(stack -> {
            for (int i = 0; i < stack.getTabCount(); i++) {
                final JComponent component = (JComponent) stack.getComponentAt(i);
                final Editor editor = EDITOR_KEY.get(component);
                editors.add(editor);
            }
        });

        return editors.toArray(Editor[]::new);
    }

    @NotNull
    @Override
    public Editor[] getEditors(@NotNull EditorStack stack) {
        final List<Editor> editors = new ArrayList<>();

        for (int i = 0; i < stack.getTabCount(); i++) {
            final JComponent component = (JComponent) stack.getComponentAt(i);
            final Editor editor = EDITOR_KEY.get(component);
            editors.add(editor);
        }

        return editors.toArray(Editor[]::new);
    }

    @Override
    public int getEditorsCount(@NotNull EditorStack stack) {
        return stack.getTabCount();
    }

    @Override
    public void closeEditor(@NotNull Editor editor) {
        final JComponent component = findEditorComponent(e -> e.equals(editor));

        if (component != null) {
            final EditorStack stack = (EditorStack) component.getParent();

            if (editor instanceof SaveableEditor se && se.isDirty()) {
                final int result = JOptionPane.showConfirmDialog(
                    getContainer(),
                    "Do you want to save changes to '%s'?".formatted(editor.getInput().getName()),
                    "Confirm Close",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );

                if (result == JOptionPane.CANCEL_OPTION) {
                    return;
                }

                if (result == JOptionPane.YES_OPTION) {
                    se.doSave(new VoidProgressMonitor());
                }
            }

            stack.remove(component);

            if (editor instanceof SaveableEditor se) {
                se.removePropertyChangeListener(this);
            }

            fireEditorChangeEvent(EditorChangeListener::editorClosed, editor);
        }
    }

    @Override
    public int getStacksCount() {
        final int[] count = new int[1];

        forEachStack(stack -> count[0] += 1);

        return count[0];
    }

    @Override
    public void addEditorChangeListener(@NotNull EditorChangeListener listener) {
        listeners.add(EditorChangeListener.class, listener);
    }

    @Override
    public void removeEditorChangeListener(@NotNull EditorChangeListener listener) {
        listeners.remove(EditorChangeListener.class, listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (SaveableEditor.PROP_DIRTY.equals(event.getPropertyName())) {
            final SaveableEditor editor = (SaveableEditor) event.getSource();
            final EditorInput input = editor.getInput();
            final JComponent component = findEditorComponent(e -> e.equals(editor));

            if (component != null) {
                final EditorStack stack = (EditorStack) component.getParent();

                if (stack != null) {
                    final int index = stack.indexOfComponent(component);

                    if (index >= 0) {
                        if (editor.isDirty()) {
                            stack.setTitleAt(index, "*" + input.getName());
                        } else {
                            stack.setTitleAt(index, input.getName());
                        }
                    }
                }
            }
        }
    }

    @NotNull
    public EditorStackContainer getContainer() {
        return container;
    }

    @Nullable
    private JComponent findEditorComponent(@NotNull Predicate<Editor> predicate) {
        for (JComponent component : getTabs()) {
            final Editor editor = EDITOR_KEY.get(component);

            if (predicate.test(editor)) {
                return component;
            }
        }

        return null;
    }

    <T> void fireEditorChangeEvent(@NotNull BiConsumer<EditorChangeListener, T> consumer, T object) {
        for (EditorChangeListener listener : listeners.getListeners(EditorChangeListener.class)) {
            consumer.accept(listener, object);
        }
    }

    @Nullable
    EditorStack getLastEditorStack() {
        return lastEditorStack;
    }

    void setLastEditorStack(@Nullable EditorStack lastEditorStack) {
        this.lastEditorStack = lastEditorStack;
    }

    @NotNull
    private EditorStack getActiveStack() {
        if (lastEditorStack != null) {
            return lastEditorStack;
        } else {
            return getActiveStack(container);
        }
    }

    @NotNull
    private JComponent[] getTabs() {
        final List<JComponent> components = new ArrayList<>();
        forEachStack(stack -> {
            for (int i = 0; i < stack.getTabCount(); i++) {
                components.add((JComponent) stack.getComponentAt(i));
            }
        });
        return components.toArray(JComponent[]::new);
    }

    @NotNull
    private EditorStack getActiveStack(@NotNull Component component) {
        if (component instanceof JSplitPane pane) {
            return getActiveStack(pane.getLeftComponent());
        } else if (component instanceof EditorStackContainer pane) {
            return getActiveStack(pane.getComponent(0));
        } else {
            return (EditorStack) component;
        }
    }

    @Nullable
    private static EditorStack getEditorStack(@NotNull Component c) {
        for (Component current = c; current != null; current = current.getParent()) {
            if (current instanceof EditorStack stack) {
                return stack;
            }
        }

        return null;
    }

    private void forEachStack(@NotNull Consumer<EditorStack> consumer) {
        forEachStack(container, consumer);
    }

    private void forEachStack(@NotNull Component component, @NotNull Consumer<EditorStack> consumer) {
        if (component instanceof JSplitPane pane) {
            forEachStack(pane.getLeftComponent(), consumer);
            forEachStack(pane.getRightComponent(), consumer);
        } else if (component instanceof EditorStackContainer container) {
            forEachStack(container.getComponent(0), consumer);
        } else {
            consumer.accept((EditorStack) component);
        }
    }

    private class LoadingWorker extends SwingWorker<EditorInput, Void> {
        private final JComponent component;
        private final LazyEditorInput input;

        public LoadingWorker(@NotNull JComponent component, @NotNull LazyEditorInput input) {
            this.component = component;
            this.input = input;
        }

        @Override
        protected EditorInput doInBackground() throws Exception {
            return input.loadRealInput(new VoidProgressMonitor());
        }

        @Override
        protected void done() {
            workers.remove(input);

            if (EDITOR_KEY.get(component) instanceof SaveableEditor se) {
                se.removePropertyChangeListener(EditorStackManager.this);
            }

            try {
                final EditorInput input = get();
                final EditorStack stack = (EditorStack) component.getParent();

                if (stack != null) {
                    final int index = stack.indexOfComponent(component);

                    if (index >= 0) {
                        final EditorProvider provider = findSuitableProvider(input);
                        final Editor editor = provider.createEditor(input);
                        final JComponent component = editor.createComponent();
                        component.putClientProperty(EDITOR_KEY, editor);

                        stack.setComponentAt(index, component);
                        stack.setTitleAt(index, input.getName());
                        stack.setToolTipTextAt(index, input.getDescription());
                        stack.setIconAt(index, provider.getIcon());

                        if (editor instanceof SaveableEditor se) {
                            se.addPropertyChangeListener(EditorStackManager.this);
                        }
                    }
                }
            } catch (Exception e) {
                closeEditor(EDITOR_KEY.get(component));
                UIUtils.showErrorDialog(e, "Unable to open editor for '%s'".formatted(input.getName()));
            }
        }
    }
}
