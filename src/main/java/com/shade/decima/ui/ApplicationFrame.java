package com.shade.decima.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatBorder;
import com.shade.decima.Project;
import com.shade.decima.ui.editors.EditorPane;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorWorkspaceNode;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.IntConsumer;

public class ApplicationFrame extends JFrame {
    private final Project project;
    private final JTabbedPane editors;

    public ApplicationFrame() {
        try {
            this.project = new Project(Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/ds.exe"));
            this.editors = new JTabbedPane();
            this.editors.setBorder(new FlatBorder());
            this.editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
            this.editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close");
            this.editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, (IntConsumer) editors::removeTabAt);
            this.editors.addChangeListener(e -> setTitle(getApplicationTitle()));

            setTitle(getApplicationTitle());
            setPreferredSize(new Dimension(640, 480));
            initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialize() {
        initializeMenuBar();
        loadProject();
    }

    private void loadProject() {
        final NavigatorWorkspaceNode workspace = new NavigatorWorkspaceNode();
        workspace.add(project);

        final DefaultTreeModel model = new DefaultTreeModel(workspace);
        final JTree navigator = new JTree(model);
        navigator.setRootVisible(false);
        navigator.setToggleClickCount(0);
        navigator.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                final Object component = event.getPath().getLastPathComponent();
                if (component instanceof NavigatorLazyNode node) {
                    node.loadChildren(model, e -> { /* currently unused */ });
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
            }
        });
        navigator.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() % 2 == 0) {
                    final int row = navigator.getRowForLocation(event.getX(), event.getY());
                    final TreePath path = navigator.getPathForLocation(event.getX(), event.getY());

                    if (row != -1 && path != null) {
                        if (navigateFromPath(path)) {
                            event.consume();
                        } else if (navigator.isExpanded(path)) {
                            navigator.collapsePath(path);
                        } else {
                            navigator.expandPath(path);
                        }
                    }
                }
            }
        });
        navigator.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER && navigateFromPath(navigator.getSelectionPath())) {
                    event.consume();
                }
            }
        });

        final JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pane.setBorder(null);
        pane.add(new JScrollPane(navigator));
        pane.add(editors);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new MigLayout("insets dialog", "[grow,fill]", "[grow,fill]"));
        contentPane.add(pane);
    }

    private boolean navigateFromPath(@Nullable TreePath path) {
        if (path != null) {
            final Object component = path.getLastPathComponent();

            if (component instanceof NavigatorFileNode file) {
                open(file);
                return true;
            }
        }

        return false;
    }

    private void open(@NotNull NavigatorFileNode node) {
        for (int i = 0; i < editors.getTabCount(); i++) {
            final EditorPane editor = (EditorPane) editors.getComponentAt(i);

            if (editor.getNode() == node) {
                editors.setSelectedComponent(editor);
                return;
            }
        }

        final EditorPane pane = new EditorPane(project, node);
        editors.addTab(node.getLabel(), pane);
        editors.setSelectedComponent(pane);
    }

    @NotNull
    private String getApplicationTitle() {
        final EditorPane editor = (EditorPane) editors.getSelectedComponent();
        if (editor != null) {
            return Application.APPLICATION_TITLE + " - " + editor.getNode().getLabel();
        } else {
            return Application.APPLICATION_TITLE;
        }
    }

    private void initializeMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        initializeFileMenu(menuBar);
        initializeEditMenu(menuBar);
        initializeHelpMenu(menuBar);

        setJMenuBar(menuBar);
    }

    private void initializeFileMenu(@NotNull JMenuBar menuBar) {
        final JMenuItem menuItemOpen = new JMenuItem("Open\u2026", KeyEvent.VK_O);
        menuItemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        menuItemOpen.addActionListener(e -> performOpenAction());

        final JMenuItem menuItemQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
        menuItemQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        menuItemQuit.addActionListener(e -> performQuitAction());

        final JMenu menuItemFile = new JMenu("File");
        menuItemFile.setMnemonic(KeyEvent.VK_F);
        menuItemFile.add(menuItemOpen);
        menuItemFile.addSeparator();
        menuItemFile.add(menuItemQuit);

        menuBar.add(menuItemFile);
    }

    private void initializeEditMenu(JMenuBar menuBar) {
        final JMenu menuItemEdit = new JMenu("Edit");
        menuItemEdit.setMnemonic(KeyEvent.VK_E);

        menuBar.add(menuItemEdit);
    }

    private void initializeHelpMenu(JMenuBar menuBar) {
        final JMenu menuItemHelp = new JMenu("Help");
        menuItemHelp.setMnemonic(KeyEvent.VK_H);

        menuBar.add(menuItemHelp);
    }

    private void performOpenAction() {
        System.out.println("To be done eventually");
    }

    private void performQuitAction() {
        dispose();
    }

    @Override
    public void dispose() {
        try {
            project.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing project", e);
        }

        super.dispose();
    }
}
