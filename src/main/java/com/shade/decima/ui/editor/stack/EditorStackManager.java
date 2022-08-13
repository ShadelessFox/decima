package com.shade.decima.ui.editor.stack;

import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorChangeListener;
import com.shade.decima.ui.editor.EditorInput;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.editor.binary.BinaryEditor;
import com.shade.decima.ui.editor.lazy.LazyEditor;
import com.shade.decima.ui.editor.lazy.LazyEditorInput;
import com.shade.decima.ui.editor.property.PropertyEditorPane;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.shade.decima.ui.CommonDataKeys.EDITOR_KEY;

public class EditorStackManager extends EditorStackContainer implements EditorManager {
    private final List<EditorChangeListener> listeners = new ArrayList<>();
    private EditorStack lastEditorStack;

    public EditorStackManager(@NotNull Workspace workspace) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(e -> {
            if ("permanentFocusOwner".equals(e.getPropertyName())) {
                final EditorStack stack = getEditorStack((Component) e.getNewValue());

                if (stack != null) {
                    lastEditorStack = stack;
                    fireEditorChangeEvent(EditorChangeListener::editorChanged, getActiveEditor());
                }
            }
        });

        workspace.addProjectChangeListener(new ProjectChangeListener() {
            @Override
            public void projectClosed(@NotNull ProjectContainer container) {
                for (Editor editor : getEditors()) {
                    final EditorInput input = editor.getInput();
                    if (!(input instanceof LazyEditorInput) && input.getProject().getContainer().equals(container)) {
                        closeEditor(editor);
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public Editor findEditor(@NotNull EditorInput input) {
        final JComponent component = findEditorComponent(e -> e.getInput().equals(input));

        if (component != null) {
            return EDITOR_KEY.get(component);
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public Editor openEditor(@NotNull EditorInput input, boolean focus) {
        return openEditor(input, true, focus);
    }

    @NotNull
    @Override
    public Editor openEditor(@NotNull EditorInput input, boolean select, boolean focus) {
        EditorStack stack;
        JComponent component = findEditorComponent(e -> e.getInput().equals(input));

        if (component == null) {
            final Editor editor = createEditor(input);

            component = editor.createComponent();
            component.putClientProperty(EDITOR_KEY, editor);

            stack = getActiveStack();
            stack.addTab(input.getName(), input.getIcon(), component, input.getDescription());

            if (input instanceof LazyEditorInput lazy) {
                new LoadingWorker(component, lazy).execute();
            }
        } else {
            stack = ((EditorStack) component.getParent());
        }

        final Editor editor = EDITOR_KEY.get(component);

        if (select && stack.getSelectedComponent() != component) {
            stack.setSelectedComponent(component);
            fireEditorChangeEvent(EditorChangeListener::editorOpened, editor);
        }

        if (focus) {
            editor.getController().getFocusComponent().requestFocusInWindow();
        }

        return editor;
    }

    @NotNull
    private Editor createEditor(@NotNull EditorInput input) {
        // TODO: Figure a better way of creating the required editor

        if (input instanceof LazyEditorInput lazy) {
            return new LazyEditor(lazy);
        }

        try {
            return new PropertyEditorPane(input);
        } catch (Exception ignored) {
            return new BinaryEditor(input);
        }
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
    public int getEditorsCount() {
        final int[] count = new int[1];

        forEachStack(stack -> count[0] += stack.getTabCount());

        return count[0];
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
            stack.remove(component);
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
        listeners.add(listener);
    }

    @Override
    public void removeEditorChangeListener(@NotNull EditorChangeListener listener) {
        listeners.remove(listener);
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

    void fireEditorChangeEvent(@NotNull BiConsumer<EditorChangeListener, Editor> consumer, Editor editor) {
        for (EditorChangeListener listener : listeners) {
            consumer.accept(listener, editor);
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
            return getActiveStack(this);
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
        forEachStack(this, consumer);
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
            final EditorInput input;

            try {
                input = get();
            } catch (Exception e) {
                closeEditor(EDITOR_KEY.get(component));
                throw new RuntimeException("Unable to initialize editor for '%s'".formatted(this.input.getName()), e);
            }

            final EditorStack stack = (EditorStack) component.getParent();
            final int index = stack.indexOfComponent(component);

            if (index >= 0) {
                final Editor editor = createEditor(input);
                final JComponent component = editor.createComponent();
                component.putClientProperty(EDITOR_KEY, editor);

                stack.setComponentAt(index, component);
                stack.setTitleAt(index, input.getName());
                stack.setToolTipTextAt(index, input.getDescription());
                stack.setIconAt(index, input.getIcon());

                if (stack.getSelectedIndex() == index) {
                    editor.getController().getFocusComponent().requestFocusInWindow();
                }
            }
        }
    }
}
