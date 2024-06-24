package com.shade.platform.ui.util;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.controls.validation.Validation;
import com.shade.platform.ui.dialogs.ExceptionDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class UIUtils {
    private UIUtils() {
    }

    @NotNull
    public static Font getDefaultFont() {
        Font font = UIManager.getFont("defaultFont");

        if (font == null) {
            font = UIManager.getFont("Label.font");
        }

        return font;
    }

    public static int getDefaultFontSize() {
        return getDefaultFont().getSize();
    }

    public static int getSmallerFontSize() {
        return UIManager.getFont("medium.font").getSize();
    }

    public static void setRenderingHints(@NotNull Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        Object aaHint = UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING);
        if (aaHint != null) {
            Object oldAA = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            if (aaHint != oldAA) {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aaHint);
            }
        }

        Object contrastHint = UIManager.get(RenderingHints.KEY_TEXT_LCD_CONTRAST);
        if (contrastHint != null) {
            Object oldContrast = g2.getRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST);
            if (contrastHint != oldContrast) {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, contrastHint);
            }
        }
    }

    public static void removeFrom(@NotNull Rectangle rect, @Nullable Insets insets) {
        if (insets != null) {
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= insets.left + insets.right;
            rect.height -= insets.top + insets.bottom;
        }
    }

    public static void removeFrom(@NotNull Dimension dimension, @Nullable Insets insets) {
        if (insets != null) {
            dimension.width -= insets.left + insets.right;
            dimension.height -= insets.top + insets.bottom;
        }
    }

    public static float getScalingFactor(float width1, float height1, float width2, float height2) {
        final float rs = width1 / height1;
        final float ri = width2 / height2;

        if (rs > ri) {
            return height1 / height2;
        } else {
            return width1 / width2;
        }
    }

    @NotNull
    public static Color mix(@NotNull Color first, @NotNull Color second, float factor) {
        if (factor <= 0.0f) {
            return first;
        } else if (factor >= 1.0f) {
            return second;
        } else {
            final float inv = 1f - factor;
            final int r = (int) (first.getRed() * inv + second.getRed() * factor);
            final int g = (int) (first.getGreen() * inv + second.getGreen() * factor);
            final int b = (int) (first.getBlue() * inv + second.getBlue() * factor);
            return new Color(r, g, b);
        }
    }

    public static void installInputValidator(@NotNull JComponent component, @NotNull InputValidator validator, @Nullable PropertyChangeListener validationListener) {
        component.setInputVerifier(validator);

        if (validationListener != null) {
            validator.getPropertyChangeSupport().addPropertyChangeListener(InputValidator.PROPERTY_VALIDATION, validationListener);
        }
    }

    public static boolean isValid(@NotNull JComponent component) {
        final InputVerifier verifier = component.getInputVerifier();
        if (verifier instanceof InputValidator validator) {
            final Validation validation = validator.getLastValidation();
            return validation == null || validation.isOK();
        }
        return true;
    }

    public static void addOpenFileAction(@NotNull JTextField component, @NotNull String title, @Nullable FileFilter filter) {
        addOpenAction(component, e -> {
            final JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle(title);
            chooser.setFileFilter(filter);
            chooser.setAcceptAllFileFilterUsed(false);

            trySetSelectedFileFromInput(component, chooser);

            if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
                return chooser.getSelectedFile().toString();
            } else {
                return null;
            }
        });
    }

    public static void addOpenDirectoryAction(@NotNull JTextField component, @NotNull String title) {
        addOpenAction(component, e -> {
            final JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle(title);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            trySetSelectedFileFromInput(component, chooser);

            if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
                return chooser.getSelectedFile().toString();
            } else {
                return null;
            }
        });
    }

    private static void trySetSelectedFileFromInput(@NotNull JTextField component, @NotNull JFileChooser chooser) {
        String text = component.getText();
        if (text.isBlank()) {
            return;
        }
        try {
            chooser.setSelectedFile(new File(text));
        } catch (Exception ignored) {
        }
    }

    public static void addOpenAction(@NotNull JTextField component, @NotNull Function<ActionEvent, String> callback) {
        addAction(component, UIManager.getIcon("Tree.openIcon"), true, e -> {
            String text = callback.apply(e);
            if (text != null) {
                component.setText(text);
            }
        });
    }

    public static void addCopyAction(@NotNull JTextField component) {
        addAction(component, UIManager.getIcon("Action.copyIcon"), false, e -> {
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            final StringSelection contents = new StringSelection(component.getText());
            clipboard.setContents(contents, contents);
        });
    }

    private static void addAction(@NotNull JTextField component, @NotNull Icon icon, boolean trackEnabledAndEditable, @NotNull ActionListener delegate) {
        final AbstractAction action = new AbstractAction(null, icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                delegate.actionPerformed(e);
            }
        };

        JToolBar toolBar = new JToolBar();
        toolBar.add(action);
        component.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);

        if (trackEnabledAndEditable) {
            toolBar.setVisible(component.isEnabled() && component.isEditable());
            component.addPropertyChangeListener("enabled", e -> toolBar.setVisible(component.isEnabled() && component.isEditable()));
        }
    }

    public static void delegateAction(@NotNull JComponent source, @NotNull JComponent target, @NotNull String targetActionKey, int targetCondition) {
        final InputMap inputMap = target.getInputMap(targetCondition);

        for (KeyStroke keyStroke : inputMap.allKeys()) {
            if (targetActionKey.equals(inputMap.get(keyStroke))) {
                delegateAction(source, keyStroke, target, targetActionKey);
            }
        }
    }

    public static void putAction(@NotNull JComponent target, int condition, @NotNull KeyStroke keystroke, @NotNull Action action) {
        target.getInputMap(condition).put(keystroke, action);
        target.getActionMap().put(action, action);
    }

    public static void installContextMenu(@NotNull JTree tree, @NotNull JPopupMenu menu) {
        installContextMenu(tree, menu, new SelectionProvider<JTree, TreePath>() {
            @Nullable
            @Override
            public TreePath getSelection(@NotNull JTree component, @Nullable MouseEvent event) {
                if (event != null) {
                    final TreePath path = component.getPathForLocation(event.getX(), event.getY());

                    if (path != null) {
                        return path;
                    }
                }

                return component.getSelectionPath();
            }

            @NotNull
            @Override
            public Point getSelectionLocation(@NotNull JTree component, @NotNull TreePath selection, @Nullable MouseEvent event) {
                if (event != null) {
                    return event.getPoint();
                } else {
                    final Rectangle bounds = Objects.requireNonNull(component.getPathBounds(selection));
                    return new Point(bounds.x, bounds.y + bounds.height);
                }
            }

            @Override
            public void setSelection(@NotNull JTree component, @NotNull TreePath selection, @Nullable MouseEvent event) {
                component.setSelectionPath(selection);

                if (event == null) {
                    component.scrollPathToVisible(selection);
                }
            }
        });
    }

    public static void installContextMenu(@NotNull JTabbedPane pane, @NotNull JPopupMenu menu) {
        installContextMenu(pane, menu, new SelectionProvider<JTabbedPane, Integer>() {
            @Nullable
            @Override
            public Integer getSelection(@NotNull JTabbedPane component, @Nullable MouseEvent event) {
                final int index;

                if (event != null) {
                    index = component.indexAtLocation(event.getX(), event.getY());
                } else {
                    index = component.getSelectedIndex();
                }

                return index < 0 ? null : index;
            }

            @NotNull
            @Override
            public Point getSelectionLocation(@NotNull JTabbedPane component, @NotNull Integer selection, @Nullable MouseEvent event) {
                if (event != null) {
                    return event.getPoint();
                } else {
                    final Rectangle bounds = Objects.requireNonNull(component.getBoundsAt(selection));
                    return new Point(bounds.x, bounds.y + bounds.height);
                }
            }

            @Override
            public void setSelection(@NotNull JTabbedPane component, @NotNull Integer selection, @Nullable MouseEvent event) {
                component.setSelectedIndex(selection);
            }
        });
    }

    public static void installContextMenu(@NotNull JComponent component, @NotNull JPopupMenu menu) {
        if (component instanceof JTree tree) {
            installContextMenu(tree, menu);
            return;
        } else if (component instanceof JTabbedPane pane) {
            installContextMenu(pane, menu);
            return;
        }

        installContextMenu(component, menu, new SelectionProvider<>() {
            @NotNull
            @Override
            public Object getSelection(@NotNull JComponent component, @Nullable MouseEvent event) {
                return component;
            }

            @NotNull
            @Override
            public Point getSelectionLocation(@NotNull JComponent component, @NotNull Object selection, @Nullable MouseEvent event) {
                if (event != null) {
                    return event.getPoint();
                } else {
                    final Rectangle bounds = component.getBounds();
                    return new Point(bounds.x, bounds.y + bounds.height);
                }
            }

            @Override
            public void setSelection(@NotNull JComponent component, @NotNull Object selection, @Nullable MouseEvent event) {
                // do nothing
            }
        });
    }

    public static <T extends JComponent, U> void installContextMenu(
        @NotNull T component,
        @NotNull JPopupMenu menu,
        @NotNull SelectionProvider<T, U> provider
    ) {
        component.setComponentPopupMenu(menu);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final U selection = provider.getSelection(component, e);

                    if (selection != null) {
                        provider.setSelection(component, selection, e);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final U selection = provider.getSelection(component, e);

                    if (selection != null) {
                        final Point location = provider.getSelectionLocation(component, selection, e);

                        menu.show(component, location.x, location.y);
                    }
                }
            }
        });

        putAction(component, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final U selection = provider.getSelection(component, null);

                if (selection != null) {
                    provider.setSelection(component, selection, null);

                    final Point location = provider.getSelectionLocation(component, selection, null);

                    menu.show(component, location.x, location.y);
                }
            }
        });
    }

    @NotNull
    public static String getLabelWithIndexMnemonic(@NotNull String label, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("negative index: " + index);
        } else if (index < 10) {
            return "&%d. %s".formatted(index < 9 ? index + 1 : 0, label);
        } else if (index < 36) {
            return "&%c. %s".formatted(index - 10 + 'A', label);
        } else {
            return label;
        }
    }

    public static void showErrorDialog(@NotNull Throwable throwable) {
        showErrorDialog(throwable, "An error occurred during program execution");
    }

    public static void showErrorDialog(@NotNull Throwable throwable, @NotNull String title) {
        showErrorDialog(JOptionPane.getRootFrame(), throwable, title);
    }

    public static void showErrorDialog(@Nullable Window parent, @NotNull Throwable throwable) {
        showErrorDialog(parent, throwable, "An error occurred during program execution");
    }

    public static void showErrorDialog(@Nullable Window parent, @NotNull Throwable throwable, @NotNull String title) {
        throwable.printStackTrace();
        new ExceptionDialog(throwable, title).showDialog(parent);
    }

    private static void delegateAction(@NotNull JComponent source, @NotNull KeyStroke sourceKeyStroke, @NotNull JComponent target, @NotNull String targetActionKey) {
        final String sourceActionKey = "delegate-" + targetActionKey;

        source.getInputMap().put(sourceKeyStroke, sourceActionKey);
        source.getActionMap().put(sourceActionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Action action = target.getActionMap().get(targetActionKey);
                if (action != null) {
                    action.actionPerformed(new ActionEvent(target, e.getID(), targetActionKey, e.getWhen(), e.getModifiers()));
                }
            }
        });
    }

    @NotNull
    public static JScrollPane createBorderlessScrollPane(@Nullable Component view) {
        return new JScrollPane(view) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createEmptyBorder());
            }
        };
    }

    @NotNull
    public static JScrollPane createScrollPane(@Nullable Component view, int top, int left, int bottom, int right) {
        return new JScrollPane(view) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, UIColor.SHADOW));
            }
        };
    }

    @NotNull
    public static JLabel createBoldLabel() {
        return createBoldLabel("");
    }

    @NotNull
    public static JLabel createBoldLabel(@NotNull String text) {
        return new JLabel(text) {
            @Override
            public void updateUI() {
                setFont(null);
                super.updateUI();
                setFont(getFont().deriveFont(Font.BOLD));
            }
        };
    }

    public static void minimizePanel(@NotNull JSplitPane pane, boolean topOrLeft) {
        try {
            final Field field = BasicSplitPaneUI.class.getDeclaredField("keepHidden");
            field.setAccessible(true);
            field.set(pane.getUI(), true);
        } catch (Exception ignored) {
            return;
        }

        pane.setLastDividerLocation(pane.getDividerLocation());
        pane.setDividerLocation(topOrLeft ? 0.0 : 1.0);
    }

    @NotNull
    public static String formatDuration(@NotNull Duration duration) {
        return "%d:%02d".formatted(duration.toMinutes(), duration.toSecondsPart());
    }

    @NotNull
    public static DataFlavor createLocalDataFlavor(@NotNull Class<?> cls) {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + cls.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error constructing flavor for " + cls, e);
        }
    }

    @NotNull
    public static String getTextForAccelerator(@NotNull KeyStroke accelerator) {
        final StringBuilder sb = new StringBuilder();

        final int modifiers = accelerator.getModifiers();
        if (modifiers != 0) {
            sb.append(InputEvent.getModifiersExText(modifiers)).append('+');
        }

        final int keyCode = accelerator.getKeyCode();
        if (keyCode != 0) {
            sb.append(KeyEvent.getKeyText(keyCode));
        } else {
            sb.append(accelerator.getKeyChar());
        }

        return sb.toString();
    }

    public static void setCursor(@NotNull Component component, @Nullable Cursor cursor) {
        if (!component.isCursorSet() || component.getCursor() != cursor) {
            component.setCursor(cursor);
        }
    }

    @NotNull
    public static JLabel createHelpToolTip(@NotNull String toolTipText) {
        JLabel label = new JLabel(UIManager.getIcon("Action.questionIcon"));
        label.setToolTipText(toolTipText);
        return label;
    }

    @NotNull
    public static JLabel createInfoLabel(@NotNull String text) {
        final JLabel label = new JLabel(text);
        label.setIcon(UIManager.getIcon("Action.informationIcon"));
        return label;
    }

    @NotNull
    public static JEditorPane createBrowseText(@NotNull String text) {
        return createText(text, new HyperlinkAdapter() {
            @Override
            public void hyperlinkActivated(@NotNull HyperlinkEvent e) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException ex) {
                    UIUtils.showErrorDialog(ex, "Unable to open the link " + e.getURL());
                }
            }
        });
    }

    @NotNull
    public static JEditorPane createText(@NotNull String text, @Nullable HyperlinkListener listener) {
        HTMLEditorKit kit = new CustomHTMLEditorKit();

        StyleSheet ss = kit.getStyleSheet();
        ss.addRule("a { text-decoration: none; }");
        ss.addRule("code { font-size: inherit; font-family: Monospaced; }");
        ss.addRule("ul { margin-left-ltr: 16px; margin-right-ltr: 16px; }");
        ss.addRule("h1 { font-size: 2em; }");
        ss.addRule("h2 { font-size: 1.5em; }");
        ss.addRule("h3 { font-size: 1.25em; }");
        ss.addRule("h4 { font-size: 1em; }");

        JEditorPane pane = new JEditorPane();
        pane.setFocusable(false);
        pane.setEditable(false);
        pane.setEditorKit(kit);
        pane.setText(text);

        if (listener != null) {
            pane.addHyperlinkListener(listener);
        }

        return pane;
    }

    public static void drawCenteredString(@NotNull Graphics g, @NotNull String text, int width, int height) {
        FontMetrics fm = g.getFontMetrics();

        int x = 0;
        int y = (height - fm.getHeight() + 1) / 2 + fm.getAscent();

        if (text.indexOf('\n') >= 0) {
            for (String line : text.split("\n")) {
                drawCenteredString(g, line, x, y, width);
                y += fm.getHeight();
            }
        } else {
            drawCenteredString(g, text, x, y, width);
        }
    }

    private static void drawCenteredString(@NotNull Graphics g, String text, int x, int y, int width) {
        g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, y);
    }

    public interface SelectionProvider<T extends JComponent, U> {
        @Nullable
        U getSelection(@NotNull T component, @Nullable MouseEvent event);

        @NotNull
        Point getSelectionLocation(@NotNull T component, @NotNull U selection, @Nullable MouseEvent event);

        void setSelection(@NotNull T component, @NotNull U selection, @Nullable MouseEvent event);
    }

    private static class CustomHTMLEditorKit extends HTMLEditorKit {
        private final LinkController linkController = new MouseExitSupportLinkController();
        private final HyperlinkListener hyperlinkListener = new LinkUnderlineListener();

        @Override
        public void install(JEditorPane c) {
            super.install(c);

            c.addHyperlinkListener(hyperlinkListener);

            List<LinkController> listeners1 = filterLinkControllerListeners(c.getMouseListeners());
            List<LinkController> listeners2 = filterLinkControllerListeners(c.getMouseMotionListeners());
            if (listeners1.size() == 1 && listeners1.equals(listeners2)) {
                LinkController oldLinkController = listeners1.get(0);
                c.removeMouseListener(oldLinkController);
                c.removeMouseMotionListener(oldLinkController);
                c.addMouseListener(linkController);
                c.addMouseMotionListener(linkController);
            }
        }

        @Override
        public void deinstall(JEditorPane c) {
            super.deinstall(c);

            c.removeHyperlinkListener(hyperlinkListener);
            c.removeMouseListener(linkController);
            c.removeMouseMotionListener(linkController);
        }

        @NotNull
        private static List<LinkController> filterLinkControllerListeners(@NotNull Object[] listeners) {
            return Arrays.stream(listeners)
                .filter(LinkController.class::isInstance)
                .map(LinkController.class::cast)
                .toList();
        }

        // Workaround for https://bugs.openjdk.org/browse/JDK-8202529
        private static final class MouseExitSupportLinkController extends HTMLEditorKit.LinkController {
            @Override
            public void mouseExited(@NotNull MouseEvent e) {
                mouseMoved(new MouseEvent(
                    e.getComponent(),
                    e.getID(),
                    e.getWhen(),
                    e.getModifiersEx(),
                    -1,
                    -1,
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getButton()
                ));
            }
        }

        private static final class LinkUnderlineListener implements HyperlinkListener {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                Element element = e.getSourceElement();
                if (element == null || "img".equals(element.getName())) {
                    return;
                }
                if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    setUnderlined(element, true);
                } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
                    setUnderlined(element, false);
                }
            }

            private static void setUnderlined(@NotNull Element element, boolean underlined) {
                if (element.getAttributes().getAttribute(HTML.Tag.A) instanceof MutableAttributeSet a) {
                    Object href = a.getAttribute(HTML.Attribute.HREF);
                    Range range = findRangeOfParentTag(element, href, HTML.Tag.A, HTML.Attribute.HREF);
                    StyledDocument document = (StyledDocument) element.getDocument();

                    a.addAttribute(CSS.Attribute.TEXT_DECORATION, underlined ? "underline" : "none");
                    document.setCharacterAttributes(range.start, range.end - range.start, a, false);
                }
            }

            @NotNull
            @SuppressWarnings("SameParameterValue")
            private static Range findRangeOfParentTag(
                @NotNull Element element,
                @Nullable Object value,
                @NotNull HTML.Tag tag,
                @NotNull HTML.Attribute attribute
            ) {
                HTMLDocument document = (HTMLDocument) element.getDocument();
                HTMLDocument.Iterator it = document.getIterator(tag);

                while (it.isValid()) {
                    if (Objects.equals(it.getAttributes().getAttribute(attribute), value)) {
                        if (it.getStartOffset() <= element.getStartOffset() && element.getStartOffset() <= it.getEndOffset()) {
                            return new Range(it.getStartOffset(), it.getEndOffset());
                        }
                    }

                    it.next();
                }

                return new Range(element.getStartOffset(), element.getEndOffset());
            }

            private record Range(int start, int end) {}
        }
    }
}
