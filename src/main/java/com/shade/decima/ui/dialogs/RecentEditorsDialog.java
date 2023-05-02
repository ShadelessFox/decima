package com.shade.decima.ui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.ui.Application;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RecentEditorsDialog extends JDialog {
    private final JTextField searchField;
    private final JList<EditorInput> resultsList;

    public RecentEditorsDialog(@NotNull JFrame frame) {
        super(frame, false);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        final EditorManager manager = Application.getEditorManager();
        final Editor activeEditor = manager.getActiveEditor();

        final EditorInput[] inputs = Arrays.stream(manager.getRecentEditors())
            .filter(editor -> editor != activeEditor)
            .map(Editor::getInput)
            .toArray(EditorInput[]::new);

        resultsList = new JList<>(new FilterableListModel(inputs));
        resultsList.setFocusable(false);
        resultsList.setSelectedIndex(0);
        resultsList.setCellRenderer(new ColoredListCellRenderer<>() {
            {
                setPadding(new Insets(2, 6, 2, 6));
            }

            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends EditorInput> list, EditorInput value, int index, boolean selected, boolean focused) {
                setLeadingIcon(UIManager.getIcon("Tree.leafIcon"));

                final String pattern = searchField.getText();
                final String text = value.getName();

                if (pattern.isEmpty()) {
                    append(text, TextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    final Range[] ranges = Objects.requireNonNull(match(pattern, text));
                    int start = 0;

                    for (Range range : ranges) {
                        append(text.substring(start, range.start), TextAttributes.REGULAR_ATTRIBUTES);
                        append(text.substring(range.start, range.end), TextAttributes.REGULAR_MATCH_ATTRIBUTES);
                        start = range.end;
                    }

                    append(text.substring(start), TextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        });
        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final int index = resultsList.locationToIndex(e.getPoint());

                if (index >= 0) {
                    openEditor(resultsList.getModel().getElementAt(index));

                    if (!e.isControlDown()) {
                        dispose();
                    }
                }
            }
        });

        searchField = new JTextField();
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter part of a file name");
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.shadow")),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshResults();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshResults();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshResults();
            }
        });

        UIUtils.delegateAction(searchField, resultsList, "selectPreviousRow", JComponent.WHEN_FOCUSED);
        UIUtils.delegateAction(searchField, resultsList, "selectNextRow", JComponent.WHEN_FOCUSED);
        UIUtils.delegateAction(searchField, resultsList, "scrollUp", JComponent.WHEN_FOCUSED);
        UIUtils.delegateAction(searchField, resultsList, "scrollDown", JComponent.WHEN_FOCUSED);

        final JScrollPane inputListPane = new JScrollPane(resultsList);
        inputListPane.setBorder(BorderFactory.createEmptyBorder());

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.shadow")));
        panel.add(searchField, BorderLayout.NORTH);
        panel.add(inputListPane, BorderLayout.CENTER);
        setContentPane(panel);

        pack();
        setLocationRelativeTo(frame);
        setVisible(true);

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final EditorInput input = resultsList.getSelectedValue();
                if (input != null) {
                    openEditor(input);
                    dispose();
                }
            }
        });

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final EditorInput input = resultsList.getSelectedValue();
                if (input != null) {
                    openEditor(input);
                }
            }
        });

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // do nothing
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                dispose();
            }
        });
    }

    private void openEditor(@NotNull EditorInput input) {
        Application.getEditorManager().openEditor(input, true);
    }

    private void refreshResults() {
        ((FilterableListModel) resultsList.getModel()).refresh(searchField.getText());
        resultsList.setSelectedIndex(0);
    }

    @Nullable
    private static Range[] match(@NotNull String pattern, @NotNull String value) {
        final List<Range> ranges = new ArrayList<>();

        for (int p = 0, v = 0, start = -1; p <= pattern.length() && v <= value.length(); v++) {
            if (p != pattern.length() && v == value.length()) {
                return null;
            }

            if (p < pattern.length() && Character.toLowerCase(pattern.charAt(p)) == Character.toLowerCase(value.charAt(v))) {
                if (start < 0) {
                    start = v;
                }

                p++;
            } else if (start >= 0) {
                ranges.add(new Range(start, v));
                start = -1;
            }
        }

        return ranges.toArray(Range[]::new);
    }

    private static class FilterableListModel extends AbstractListModel<EditorInput> {
        private final EditorInput[] input;
        private EditorInput[] results;

        public FilterableListModel(@NotNull EditorInput[] input) {
            this.input = input;
            this.results = input;
        }

        @Override
        public int getSize() {
            return results.length;
        }

        @Override
        public EditorInput getElementAt(int index) {
            return results[index];
        }

        public void refresh(@NotNull String pattern) {
            if (pattern.isEmpty()) {
                this.results = input;
            } else {
                this.results = Arrays.stream(input)
                    .filter(input -> match(pattern, input.getName()) != null)
                    .toArray(EditorInput[]::new);
            }

            fireContentsChanged(this, 0, results.length - 1);
        }
    }

    private record Range(int start, int end) {}
}
