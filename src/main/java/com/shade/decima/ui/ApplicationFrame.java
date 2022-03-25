package com.shade.decima.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatBorder;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.actions.Actions;
import com.shade.decima.ui.editors.EditorPane;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.dnd.FileTransferHandler;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorWorkspaceNode;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.function.IntConsumer;

public class ApplicationFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(ApplicationFrame.class);

    private final Workspace workspace;
    private final NavigatorTree navigator;
    private final JTabbedPane editors;
    private EditorPane focusedEditor;
    private EditorPane activeEditor;

    public ApplicationFrame() {
        try {
            this.workspace = new Workspace();
            this.navigator = new NavigatorTree(workspace, new NavigatorWorkspaceNode(workspace));
            this.editors = new JTabbedPane();

            setTitle(getApplicationTitle());
            setPreferredSize(new Dimension(640, 480));

            initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialize() {
        initializeMenuBar();
        initializeNavigatorPane();
        initializeEditorsPane();

        final JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pane.setBorder(null);
        pane.add(navigator);
        pane.add(editors);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new MigLayout("insets dialog", "[grow,fill]", "[grow,fill]"));
        contentPane.add(pane);
    }

    private void initializeEditorsPane() {
        editors.setBorder(new FlatBorder());
        editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close");
        editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, (IntConsumer) editors::removeTabAt);
        editors.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        editors.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        editors.addChangeListener(e -> setActiveEditor((EditorPane) editors.getSelectedComponent()));
        editors.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                final int index = editors.indexAtLocation(e.getX(), e.getY());
                if (SwingUtilities.isRightMouseButton(e) && index >= 0) {
                    focusedEditor = (EditorPane) editors.getComponentAt(index);
                    final JPopupMenu menu = new JPopupMenu();
                    Actions.contribute(menu, "popup:editor");
                    menu.show(editors, e.getX(), e.getY());
                }
            }
        });
    }

    private void initializeNavigatorPane() {
        final JTree tree = navigator.getTree();
        tree.setRootVisible(false);
        tree.setToggleClickCount(0);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() % 2 == 0) {
                    final int row = tree.getRowForLocation(event.getX(), event.getY());
                    final TreePath path = tree.getPathForLocation(event.getX(), event.getY());

                    if (row != -1 && path != null) {
                        if (navigateFromPath(path)) {
                            event.consume();
                        } else if (tree.isExpanded(path)) {
                            tree.collapsePath(path);
                        } else {
                            tree.expandPath(path);
                        }
                    }
                }
            }
        });
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER && navigateFromPath(tree.getSelectionPath())) {
                    event.consume();
                }
            }
        });
        tree.setTransferHandler(new FileTransferHandler());
        tree.setDropTarget(null);
        tree.setDragEnabled(true);
    }

    @NotNull
    public NavigatorTree getNavigator() {
        return navigator;
    }

    @NotNull
    public JTabbedPane getEditorsPane() {
        return editors;
    }

    @Nullable
    public EditorPane getFocusedEditor() {
        return focusedEditor;
    }

    public void setActiveEditor(@Nullable EditorPane activeEditor) {
        this.activeEditor = activeEditor;
        setTitle(getApplicationTitle());
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
                editors.requestFocusInWindow();
                return;
            }
        }

        final EditorPane pane = new EditorPane(UIUtils.getProject(node), node);
        editors.addTab(node.toString(), pane);
        editors.setSelectedComponent(pane);
        editors.requestFocusInWindow();
    }

    @NotNull
    private String getApplicationTitle() {
        if (activeEditor != null) {
            return Application.APPLICATION_TITLE + " - " + activeEditor.getNode();
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
        final JMenu menuItemFile = new JMenu("File");
        menuItemFile.setMnemonic(KeyEvent.VK_F);

        Actions.contribute(menuItemFile, "menu:file");

        menuBar.add(menuItemFile);
    }

    private void initializeEditMenu(JMenuBar menuBar) {
        final JMenu menuItemEdit = new JMenu("Edit");
        menuItemEdit.setMnemonic(KeyEvent.VK_E);

        Actions.contribute(menuItemEdit, "menu:edit");

        menuBar.add(menuItemEdit);
    }

    private void initializeHelpMenu(JMenuBar menuBar) {
        final JMenu menuItemHelp = new JMenu("Help");
        menuItemHelp.setMnemonic(KeyEvent.VK_H);

        Actions.contribute(menuItemHelp, "menu:help");

        menuBar.add(menuItemHelp);
    }

    @Override
    public void dispose() {
        try {
            workspace.close();
        } catch (IOException e) {
            log.error("Error closing workspace", e);
        }

        super.dispose();
    }
}
