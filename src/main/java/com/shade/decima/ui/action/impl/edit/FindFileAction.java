package com.shade.decima.ui.action.impl.edit;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.rtti.objects.RTTICollection;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// TODO: This action is always enabled despite the context surrounding it.
//       Although we make sure that currently selected node is present and
//       loaded, it would be cool if the action API could handle it automatically

@ActionRegistration(id = "com.shade.decima.ui.action.impl.edit.FindFileAction", name = "Find &Files\u2026", description = "Find files that match the string", accelerator = "ctrl shift F")
@ActionContribution(path = "menu:edit")
public class FindFileAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final NavigatorTree navigator = Application.getFrame().getNavigator();

        if (navigator.getTree().getLastSelectedPathComponent() instanceof NavigatorNode node) {
            final NavigatorProjectNode root = UIUtils.getParentNode(node, NavigatorProjectNode.class);
            if (root == null || root.needsInitialization()) {
                return;
            }
            new FindFileDialog(Application.getFrame(), root).setVisible(true);
        }
    }

    private static class FindFileDialog extends JDialog {

        public FindFileDialog(@NotNull JFrame frame, @NotNull NavigatorNode root) {
            super(frame, "Find files", true);

            final List<String> files = new ArrayList<>();
            final Project project = UIUtils.getProject(root);

            try {
                final RTTIObject prefetch = project.getArchiveManager()
                    .readFileObjects(project.getCompressor(), "prefetch/fullgame.prefetch")
                    .get(0);

                for (RTTIObject file : prefetch.<RTTICollection<RTTIObject>>get("Files")) {
                    files.add(file.get("Path"));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final JList<String> resultList = new JList<>();
            resultList.setModel(new ListSearchModel(files, 100));
            resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            resultList.setFocusable(false);

            final JTextField searchField = new JTextField();
            searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter part of a name");
            searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    ((ListSearchModel) resultList.getModel()).refresh(searchField.getText());
                    resultList.setSelectedIndex(0);
                }
            });

            UIUtils.delegateKey(searchField, resultList, KeyEvent.VK_UP, "selectPreviousRow");
            UIUtils.delegateKey(searchField, resultList, KeyEvent.VK_DOWN, "selectNextRow");
            UIUtils.delegateKey(searchField, resultList, KeyEvent.VK_PAGE_UP, "scrollUp");
            UIUtils.delegateKey(searchField, resultList, KeyEvent.VK_PAGE_DOWN, "scrollDown");
            UIUtils.delegateKey(searchField, resultList, KeyEvent.VK_HOME, "selectFirstRow");
            UIUtils.delegateKey(searchField, resultList, KeyEvent.VK_END, "selectLastRow");

            final JPanel panel = new JPanel();
            panel.setLayout(new MigLayout("ins dialog", "[fill,grow]", "[][fill,grow]"));
            panel.add(searchField, "wrap");
            panel.add(new JScrollPane(resultList));
            getContentPane().add(panel);

            pack();
            searchField.requestFocusInWindow();

            setSize(550, 350);
            setLocationRelativeTo(Application.getFrame());
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            final JRootPane rootPane = getRootPane();

            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
            rootPane.getActionMap().put("close", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openAndClose");
            rootPane.getActionMap().put("openAndClose", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    final String path = resultList.getSelectedValue();
                    if (path != null) {
                        openSelectedFile(path, root, project);
                        setVisible(false);
                    }
                }
            });

            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "openAndKeep");
            rootPane.getActionMap().put("openAndKeep", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    final String path = resultList.getSelectedValue();
                    if (path != null) {
                        openSelectedFile(path, root, project);
                    }
                }
            });
        }

        private void openSelectedFile(@NotNull String path, @NotNull NavigatorNode root, @NotNull Project project) {
            final Archive.FileEntry entry = project.getArchiveManager().getFileEntry(path);
            if (entry == null) {
                return;
            }

            final NavigatorNode target;

            try {
                target = Application.getFrame().getNavigator().findNode(new VoidProgressMonitor(), root, path.split("/"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (target instanceof NavigatorFileNode file) {
                Application.getFrame().getEditorsPane().showEditor(file);
            }
        }
    }

    private static class ListSearchModel extends AbstractListModel<String> {
        private final List<String> choices;
        private final List<String> results;
        private final int limit;

        public ListSearchModel(@NotNull List<String> choices, int limit) {
            this.choices = choices;
            this.results = new ArrayList<>();
            this.limit = limit;
        }

        public void refresh(@NotNull String query) {
            final int size = results.size();

            results.clear();
            fireIntervalRemoved(this, 0, size);

            if (query.isEmpty()) {
                return;
            }

            final List<String> output = choices.stream()
                .filter(x -> x.contains(query))
                .limit(limit)
                .toList();

            if (output.isEmpty()) {
                return;
            }

            results.addAll(output);
            fireIntervalAdded(this, 0, this.results.size());
        }

        @Override
        public int getSize() {
            return results.size();
        }

        @Override
        public String getElementAt(int index) {
            return results.get(index);
        }
    }
}
