package com.shade.platform.ui.editors.stack;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.editors.*;
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
    private static final DataKey<EditorInput> NEW_INPUT_KEY = new DataKey<>("newInput", EditorInput.class);

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
                Application.getMenuService().installPopupMenu(stack, MenuConstants.CTX_MENU_EDITOR_STACK_ID, key -> switch (key) {
                    case "editor" -> getActiveEditor();
                    case "editorStack" -> stack;
                    case "editorManager" -> EditorStackManager.this;
                    default -> null;
                });

                stack.addChangeListener(e -> {
                    final int index = stack.getSelectedIndex();

                    if (index >= 0 && stack.getComponentAt(index) instanceof PlaceholderComponent placeholder) {
                        final Editor editor = EDITOR_KEY.get(placeholder);
                        final JComponent component = editor.createComponent();

                        component.putClientProperty(EDITOR_KEY, editor);
                        stack.setComponentAt(index, component);
                    }
                });
            }

            @Override
            public void editorChanged(@Nullable Editor editor) {
                if (editor == null) {
                    return;
                }

                final JComponent component = findEditorComponent(editor::equals);

                if (component != null) {
                    final EditorInput input = (EditorInput) component.getClientProperty(NEW_INPUT_KEY);

                    if (input != null) {
                        component.putClientProperty(NEW_INPUT_KEY, null);
                        handleEditorInputChanged(editor, input);
                    }
                }
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
        return openEditor(input, provider, stack, select, focus, -1);
    }

    @NotNull
    @Override
    public Editor openEditor(@NotNull EditorInput input, @Nullable EditorProvider provider, @Nullable EditorStack stack, boolean select, boolean focus, int index) {
        JComponent component = findEditorComponent(e -> e.getInput().representsSameResource(input));

        if (component == null) {
            final Editor editor;

            if (provider == null) {
                final var result = createEditorForInput(input);
                editor = result.editor();
                provider = result.provider();
            } else {
                editor = provider.createEditor(input);
            }

            if (editor instanceof SaveableEditor se) {
                se.addPropertyChangeListener(this);
            }

            component = select ? editor.createComponent() : new PlaceholderComponent();
            component.putClientProperty(EDITOR_KEY, editor);

            stack = Objects.requireNonNullElseGet(stack, this::getActiveStack);
            stack.insertTab(input.getName(), provider.getIcon(), component, input.getDescription(), index < 0 ? stack.getTabCount() : index);
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

    @Nullable
    @Override
    public Editor reuseEditor(@NotNull Editor oldEditor, @NotNull EditorInput newInput) {
        final JComponent oldComponent = findEditorComponent(e -> e.equals(oldEditor));

        if (oldComponent != null) {
            final EditorStack stack = (EditorStack) oldComponent.getParent();

            if (stack != null) {
                final int index = stack.indexOfComponent(oldComponent);
                final boolean selected = stack.getSelectedIndex() == index;

                if (index >= 0) {
                    if (oldEditor instanceof SaveableEditor se) {
                        se.removePropertyChangeListener(this);
                    }

                    final EditorResult result = createEditorForInput(newInput);

                    if (result.editor() instanceof SaveableEditor se) {
                        se.addPropertyChangeListener(EditorStackManager.this);
                    }

                    if (oldEditor instanceof StatefulEditor o && result.editor() instanceof StatefulEditor n) {
                        final Map<String, Object> state = new HashMap<>();

                        o.saveState(state);

                        if (!state.isEmpty()) {
                            n.loadState(state);
                        }
                    }

                    final JComponent newComponent = selected ? result.editor().createComponent() : new PlaceholderComponent();
                    newComponent.putClientProperty(EDITOR_KEY, result.editor());

                    stack.setComponentAt(index, newComponent);
                    stack.setTitleAt(index, newInput.getName());
                    stack.setToolTipTextAt(index, newInput.getDescription());
                    stack.setIconAt(index, result.provider().getIcon());

                    if (oldEditor.isFocused()) {
                        newComponent.validate();
                        result.editor().setFocus();
                    }

                    return result.editor();
                }
            }
        }

        return null;
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

                if (result == JOptionPane.YES_OPTION) {
                    se.doSave(new VoidProgressMonitor());
                } else if (result != JOptionPane.NO_OPTION) {
                    return;
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
    public void notifyInputChanged(@NotNull EditorInput input) {
        forEachStack(stack -> {
            for (int i = 0; i < stack.getTabCount(); i++) {
                final JComponent component = (JComponent) stack.getComponentAt(i);
                final Editor editor = EDITOR_KEY.get(component);

                if (editor.getInput().representsSameResource(input)) {
                    if (stack.getSelectedIndex() == i) {
                        handleEditorInputChanged(editor, input);
                    } else {
                        component.putClientProperty(NEW_INPUT_KEY, input);
                    }
                }
            }
        });
    }

    private void handleEditorInputChanged(@NotNull Editor editor, @NotNull EditorInput input) {
        final int result = JOptionPane.showConfirmDialog(
            container,
            "The file '%s' has been changed.\n\nDo you want to replace the editor contents with these changes?".formatted(input.getName()),
            "Confirm Update",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            reuseEditor(editor, input);
        } else if (editor instanceof SaveableEditor e) {
            e.setDirty(true);
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

    @NotNull
    private EditorResult createEditorForInput(@NotNull EditorInput input) {
        final var providers = EDITOR_PROVIDERS.stream()
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.matches(input) != EditorProvider.Match.NONE)
            .sorted(Comparator.comparing(provider -> provider.matches(input)))
            .toList();

        Exception exception = null;

        for (EditorProvider provider : providers) {
            try {
                return new EditorResult(provider.createEditor(input), provider);
            } catch (Exception e) {
                exception = e;
            }
        }

        throw new IllegalArgumentException("Unable to find a suitable editor for input: " + input, exception);
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

    private static class PlaceholderComponent extends JComponent {}

    private static record EditorResult(@NotNull Editor editor, @NotNull EditorProvider provider) {}
}
