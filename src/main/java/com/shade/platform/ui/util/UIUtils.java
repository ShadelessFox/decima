package com.shade.platform.ui.util;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.controls.validation.Validation;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Objects;

public final class UIUtils {
    private UIUtils() {
    }

    @NotNull
    public static Color getInactiveTextColor() {
        return UIManager.getColor("Label.disabledForeground");
    }

    public static float getSmallerFontSize() {
        final Font font = UIManager.getFont("Label.font");
        return Math.max(font.getSize() - UIScale.scale(2f), UIScale.scale(11f));
    }

    public static void setRenderingHints(@NotNull Graphics2D g) {
        final Object textAliasing = UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING);
        final Object lcdContrast = UIManager.get(RenderingHints.KEY_TEXT_LCD_CONTRAST);

        if (textAliasing != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAliasing);
        }

        if (lcdContrast != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, lcdContrast);
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
        final JToolBar toolBar = new JToolBar();

        toolBar.add(new AbstractAction(null, UIManager.getIcon("Tree.openIcon")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();

                chooser.setDialogTitle(title);
                chooser.setFileFilter(filter);
                chooser.setAcceptAllFileFilterUsed(false);

                if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
                    component.setText(chooser.getSelectedFile().toString());
                }
            }
        });

        component.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);
    }

    public static void addOpenDirectoryAction(@NotNull JTextField component, @NotNull String title) {
        final JToolBar toolBar = new JToolBar();

        toolBar.add(new AbstractAction(null, UIManager.getIcon("Tree.openIcon")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();

                chooser.setDialogTitle(title);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
                    component.setText(chooser.getSelectedFile().toString());
                }
            }
        });

        component.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);
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

    public static void delegateKey(@NotNull JComponent source, int sourceKeyCode, @NotNull JComponent target, @NotNull String targetActionKey) {
        delegateAction(source, KeyStroke.getKeyStroke(sourceKeyCode, 0), target, targetActionKey);
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

    public static void installPopupMenu(@NotNull JTree tree, @NotNull JPopupMenu menu) {
        installPopupMenu(tree, menu, new SelectionProvider<JTree, TreePath>() {
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
                    return new Point(event.getX(), event.getY());
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

    public static void installPopupMenu(@NotNull JTabbedPane pane, @NotNull JPopupMenu menu) {
        installPopupMenu(pane, menu, new SelectionProvider<JTabbedPane, Integer>() {
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
                    return new Point(event.getX(), event.getY());
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

    public static <T extends JComponent, U> void installPopupMenu(
        @NotNull T component,
        @NotNull JPopupMenu menu,
        @NotNull SelectionProvider<T, U> provider
    ) {
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

    public static void showErrorDialog(@Nullable Window parent, @NotNull Throwable throwable) {
        showErrorDialog(parent, throwable, "An error occurred during program execution");
    }

    public static void showErrorDialog(@Nullable Window parent, @NotNull Throwable throwable, @NotNull String title) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);

        final JTextArea view = new JTextArea(sw.toString().replace("\t", "    "));
        view.setFont(new Font(Font.MONOSPACED, view.getFont().getStyle(), view.getFont().getSize()));
        view.setEditable(false);

        final JScrollPane pane = new JScrollPane(view);
        pane.setPreferredSize(new Dimension(640, 480));

        JOptionPane.showMessageDialog(parent, pane, title, JOptionPane.ERROR_MESSAGE);
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

    public interface SelectionProvider<T extends JComponent, U> {
        @Nullable
        U getSelection(@NotNull T component, @Nullable MouseEvent event);

        @NotNull
        Point getSelectionLocation(@NotNull T component, @NotNull U selection, @Nullable MouseEvent event);

        void setSelection(@NotNull T component, @NotNull U selection, @Nullable MouseEvent event);
    }
}
