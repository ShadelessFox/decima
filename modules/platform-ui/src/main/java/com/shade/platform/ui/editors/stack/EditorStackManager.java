package com.shade.platform.ui.editors.stack;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shade.platform.model.SaveableElement;
import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.*;
import com.shade.platform.ui.editors.spi.EditorOnboarding;
import com.shade.platform.ui.editors.spi.EditorOnboardingProvider;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

import static com.shade.platform.ui.PlatformDataKeys.EDITOR_KEY;
import static com.shade.platform.ui.PlatformMenuConstants.CTX_MENU_EDITOR_STACK_ID;

@Service(EditorManager.class)
@Persistent("EditorManager")
public class EditorStackManager implements EditorManager, PropertyChangeListener, PersistableComponent<EditorStackManager.Container> {
    private static final Logger log = LoggerFactory.getLogger(EditorStackManager.class);
    private static final Gson gson = new Gson();

    private static final ServiceLoader<EditorProvider> EDITOR_PROVIDERS = ServiceLoader.load(EditorProvider.class);
    private static final DataKey<EditorInput> NEW_INPUT_KEY = new DataKey<>("newInput", EditorInput.class);
    private static final DataKey<Long> LAST_USAGE_KEY = new DataKey<>("lastUsage", Long.class);

    private static final Comparator<JComponent> MRU_COMPARATOR = Comparator.comparing(component -> {
        final Object lastUsage = component.getClientProperty(LAST_USAGE_KEY);
        if (lastUsage != null) {
            return -LAST_USAGE_KEY.cast(lastUsage);
        } else {
            return 0L;
        }
    });

    private final EditorStackContainer container;
    private EditorStack lastEditorStack;

    public EditorStackManager() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(e -> {
            if ("permanentFocusOwner".equals(e.getPropertyName())) {
                final EditorStack stack = getEditorStack((Component) e.getNewValue());

                if (stack != null) {
                    lastEditorStack = stack;
                    MessageBus.getInstance().publisher(EditorManager.EDITORS).editorChanged(getActiveEditor());
                }
            }
        });

        MessageBus.getInstance().connect().subscribe(EDITORS, new EditorChangeListener() {
            @Override
            public void editorStackCreated(@NotNull EditorStack stack) {
                MenuManager.getInstance().installContextMenu(stack, CTX_MENU_EDITOR_STACK_ID, key -> switch (key) {
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

                    component.putClientProperty(LAST_USAGE_KEY, System.currentTimeMillis());
                }
            }
        });

        this.container = new RootEditorStackContainer(this);
    }

    @Nullable
    @Override
    public Editor findEditor(@NotNull EditorInput input) {
        final JComponent component = findEditorComponent(e -> input.representsSameResource(e.getInput()));

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
        JComponent component = findEditorComponent(e -> input.representsSameResource(e.getInput()));

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
            component.putClientProperty(LAST_USAGE_KEY, System.currentTimeMillis());

            stack = Objects.requireNonNullElseGet(stack, this::getActiveStack);
            stack.insertTab(input.getName(), provider.getIcon(), component, input.getDescription(), index < 0 ? stack.getSelectedIndex() + 1 : index);
        } else {
            stack = ((EditorStack) component.getParent());
        }

        final Editor editor = EDITOR_KEY.get(component);

        if (select && stack.getSelectedComponent() != component) {
            // HACK: Prevent focus from being transferred if not required
            stack.setFocusable(false);
            stack.setSelectedComponent(component);
            stack.setFocusable(true);

            MessageBus.getInstance().publisher(EditorManager.EDITORS).editorOpened(editor);
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

                    if (!(oldComponent instanceof PlaceholderComponent)) {
                        oldEditor.dispose();
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

                    if (selected && oldEditor.isFocused()) {
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

    @NotNull
    @Override
    public Editor[] getRecentEditors() {
        final List<JComponent> components = new ArrayList<>();

        forEachStack(stack -> {
            for (int i = 0; i < stack.getTabCount(); i++) {
                components.add((JComponent) stack.getComponentAt(i));
            }
        });

        return components.stream()
            .sorted(MRU_COMPARATOR)
            .map(EDITOR_KEY::get)
            .toArray(Editor[]::new);
    }

    @Override
    public int getEditorsCount() {
        final int[] count = {0};

        forEachStack(stack -> count[0] += getEditorsCount(stack));

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

            if (!(component instanceof PlaceholderComponent)) {
                editor.dispose();
            }

            MessageBus.getInstance().publisher(EditorManager.EDITORS).editorClosed(editor);
        }
    }

    @Override
    public void notifyInputChanged(@NotNull EditorInput input) {
        forEachStack(stack -> {
            for (int i = 0; i < stack.getTabCount(); i++) {
                final JComponent component = (JComponent) stack.getComponentAt(i);
                final Editor editor = EDITOR_KEY.get(component);

                if (input.representsSameResource(editor.getInput())) {
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

    @Override
    @NotNull
    public EditorStackContainer getContainer() {
        return container;
    }

    @Nullable
    @Override
    public Container getState() {
        return saveState(container);
    }

    @Override
    public void loadState(@NotNull Container state) {
        restoreState(state, this, container);
    }

    @Override
    public void noStateLoaded() {
        // Backward compatibility
        final Preferences node = Preferences.userRoot().node("decima-explorer/editors");

        try {
            if (node.childrenNames().length > 0) {
                loadState(restoreLegacyState(node));
            }
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private static Container saveState(@NotNull EditorStackContainer container) {
        if (container.isSplit()) {
            final Container left = saveState(container.getLeftContainer());
            final Container right = saveState(container.getRightContainer());

            if (left == null || right == null) {
                if (left == null) {
                    return right;
                } else {
                    return left;
                }
            }

            return new Container(new Split(
                Orientation.valueOf(container.getSplitOrientation()),
                container.getSplitPosition(),
                left,
                right
            ));
        } else {
            final File[] files = Arrays.stream(container.getChildren())
                .map(EditorStackManager::saveState)
                .filter(Objects::nonNull)
                .toArray(File[]::new);

            if (files.length == 0) {
                return null;
            }

            return new Container(files);
        }
    }

    @Nullable
    private static File saveState(@NotNull Component component) {
        final EditorStack stack = (EditorStack) component.getParent();
        final Editor editor = PlatformDataKeys.EDITOR_KEY.get((JComponent) component);
        final EditorInput input = editor.getInput();

        if (!(input instanceof SaveableElement element)) {
            return null;
        }

        final Map<String, Object> inputMap = new HashMap<>();
        final Map<String, Object> stateMap = new HashMap<>();

        element.saveState(inputMap);

        if (editor instanceof StatefulEditor se) {
            try {
                se.saveState(stateMap);
            } catch (Exception e) {
                log.error("Unable to save state of editor '" + se + "' with input '" + se.getInput() + "'", e);
                return null;
            }
        }

        return new File(
            stack.getSelectedComponent() == component,
            element.getFactoryId(),
            inputMap,
            stateMap.isEmpty() ? null : stateMap
        );
    }

    private static void restoreState(@NotNull Container state, @NotNull EditorStackManager manager, @NotNull EditorStackContainer container) {
        if (state.split != null) {
            final var split = state.split;
            final var orientation = split.orientation.ordinal();
            final var proportion = split.proportion;
            final var result = container.split(orientation, proportion, false);

            restoreState(split.first, manager, result.leading());
            restoreState(split.second, manager, result.trailing());
        } else if (state.stack != null) {
            final var files = state.stack;
            final var stack = (EditorStack) container.getComponent(0);

            if (files.length == 0) {
                return;
            }

            int selection = -1;

            for (int i = 0; i < files.length; i++) {
                if (files[i].selected) {
                    selection = i;
                    break;
                }
            }

            if (selection >= 0) {
                restoreState(files[selection], manager, stack, 0);
            }

            for (int i = 0, j = 0; i < files.length; i++) {
                if (i != selection) {
                    if (restoreState(files[i], manager, stack, j)) {
                        j++;
                    }
                }
            }
        } else {
            log.warn("State has no 'split' nor 'stack' element");
        }
    }

    private static boolean restoreState(@NotNull File file, @NotNull EditorManager manager, @NotNull EditorStack stack, int index) {
        final var factory = ApplicationManager.getApplication().getElementFactory(file.factory);

        if (factory == null) {
            return false;
        }

        final var input = (EditorInput) factory.createElement(file.input);
        final var editor = manager.openEditor(input, null, stack, file.selected, file.selected, index);

        if (file.state != null && editor instanceof StatefulEditor se) {
            try {
                se.loadState(file.state);
            } catch (Exception e) {
                log.error("Unable to restore state of editor '" + se + "' with input '" + input + "'", e);
            }
        }

        return true;
    }

    @NotNull
    private static Container restoreLegacyState(@NotNull Preferences pref) {
        final String type = pref.get("type", "stack");
        final Preferences[] children = IOUtils.children(pref);
        Arrays.sort(children, Comparator.comparingInt(p -> Integer.parseInt(p.name())));

        if (type.equals("split")) {
            final var orientation = pref.get("orientation", "horizontal").equals("horizontal") ? Orientation.HORIZONTAL : Orientation.VERTICAL;
            final var proportion = pref.getDouble("position", 0.5);

            return new Container(new Split(
                orientation,
                proportion,
                restoreLegacyState(children[0]),
                restoreLegacyState(children[1])
            ));
        } else {
            final int selection = pref.getInt("selection", -1);
            final File[] files = new File[children.length];

            for (int i = 0; i < children.length; i++) {
                files[i] = restoreLegacyState(children[i], selection == i);
            }

            return new Container(files);
        }
    }

    @NotNull
    private static File restoreLegacyState(@NotNull Preferences pref, boolean selected) {
        final var factory = pref.get("$factory_id", "com.shade.decima.ui.editor.NodeEditorInputFactory");
        final var state = pref.get("state", null);

        Map<String, Object> stateMap = null;
        final Map<String, Object> inputMap = new HashMap<>();

        if (state != null) {
            try {
                stateMap = gson.fromJson(state, new TypeToken<Map<String, Object>>() {}.getType());
            } catch (Exception e) {
                log.error("Unable to restore state of editor", e);
            }
        }

        for (String key : IOUtils.unchecked(pref::keys)) {
            if (key.equals("$factory_id") || key.equals("state")) {
                // Special attributes
                continue;
            }

            final String value = pref.get(key, null);

            if (value != null) {
                inputMap.put(key, value);
            }
        }

        return new File(selected, factory, inputMap, stateMap);
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

    protected record Container(@Nullable File[] stack, @Nullable Split split) {
        public Container(@NotNull File[] files) {
            this(files, null);
        }

        public Container(@NotNull Split split) {
            this(null, split);
        }
    }

    private record File(boolean selected, @NotNull String factory, @NotNull Map<String, Object> input, @Nullable Map<String, Object> state) {}

    private record Split(@NotNull Orientation orientation, double proportion, @NotNull Container first, @NotNull Container second) {}

    private enum Orientation {
        VERTICAL,
        HORIZONTAL;

        @NotNull
        public static Orientation valueOf(int value) {
            return switch (value) {
                case JSplitPane.HORIZONTAL_SPLIT -> HORIZONTAL;
                case JSplitPane.VERTICAL_SPLIT -> VERTICAL;
                default -> throw new IllegalArgumentException(String.valueOf(value));
            };
        }
    }

    private static class PlaceholderComponent extends JComponent {}

    private record EditorResult(@NotNull Editor editor, @NotNull EditorProvider provider) {}

    private static class RootEditorStackContainer extends EditorStackContainer {
        private static final OnboardingElement[] onboardingElements = availableElements();

        RootEditorStackContainer(@NotNull EditorStackManager manager) {
            super(manager, null);

            addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        removeHierarchyListener(this);
                        layoutContainer();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (isLeaf() && asEditorStack().getTabCount() == 0) {
                final Graphics2D g2 = (Graphics2D) g.create();
                try {
                    UIUtils.setRenderingHints(g2);
                    paintOnboarding(g2);
                } finally {
                    g2.dispose();
                }
            }
        }

        private void paintOnboarding(@NotNull Graphics2D g2) {
            g2.setFont(UIManager.getFont("Onboarding.font"));

            final FontMetrics metrics = g2.getFontMetrics();
            final int spacing = metrics.getHeight() + metrics.getDescent();

            int width = 0;
            int height = 0;

            for (OnboardingElement element : onboardingElements) {
                if (element.description() != null) {
                    width = Math.max(width, metrics.stringWidth(element.text() + ' ' + element.description()));
                } else {
                    width = Math.max(width, metrics.stringWidth(element.text()));
                }

                height += spacing;
            }

            // To make the text positioned relatively to the center of the window, not the editor stack
            final var parent = getParent();
            final int deltaX = parent.getWidth() - getWidth();
            final int deltaY = parent.getHeight() - getHeight();

            for (int i = 0; i < onboardingElements.length; i++) {
                final OnboardingElement element = onboardingElements[i];

                final int x = (getWidth() - width - deltaX) / 2;
                final int y = (getHeight() - height - deltaY) / 2 + i * spacing + metrics.getAscent();

                g2.setColor(UIManager.getColor("Onboarding.textForeground"));
                g2.drawString(element.text(), x, y);

                if (element.description() != null) {
                    final int shift = metrics.stringWidth(element.text()) + metrics.charWidth(' ');
                    g2.setColor(UIManager.getColor("Onboarding.descriptionForeground"));
                    g2.drawString(element.description(), x + shift, y);
                }
            }
        }

        @NotNull
        private static OnboardingElement[] availableElements() {
            final List<OnboardingElement> elements = new ArrayList<>();

            for (EditorOnboardingProvider provider : ServiceLoader.load(EditorOnboardingProvider.class)) {
                for (EditorOnboarding onboarding : provider.getOnboardings()) {
                    final OnboardingElement element = resolveElement(onboarding);
                    if (element == null) {
                        continue;
                    }
                    elements.add(element);
                }
            }

            return elements.toArray(OnboardingElement[]::new);
        }

        @Nullable
        private static OnboardingElement resolveElement(@NotNull EditorOnboarding onboarding) {
            if (onboarding instanceof EditorOnboarding.Action action) {
                final MenuItemRegistration item = MenuManager.getInstance().findItem(action.id());
                if (item == null) {
                    log.warn("Unable to resolve onboarding command: " + action.id());
                    return null;
                }

                final String name;
                if (action.name() == null) {
                    name = item.name();
                } else {
                    name = action.name();
                }

                final String description;
                if (item.keystroke().isEmpty()) {
                    description = null;
                } else {
                    description = UIUtils.getTextForAccelerator(KeyStroke.getKeyStroke(item.keystroke()));
                }

                return new OnboardingElement(name, description);
            } else if (onboarding instanceof EditorOnboarding.Text text) {
                return new OnboardingElement(text.text(), null);
            } else {
                return null;
            }
        }

        private record OnboardingElement(@NotNull String text, @Nullable String description) {}
    }
}
