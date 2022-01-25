package com.shade.decima.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatBorder;
import com.shade.decima.base.GameType;
import com.shade.decima.ui.editors.EditorPane;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.decima.ui.navigator.impl.NavigatorWorkspaceNode;
import com.shade.decima.ui.resources.Project;
import com.shade.decima.ui.resources.Workspace;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.IntConsumer;

public class ApplicationFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(ApplicationFrame.class);

    private final Workspace workspace;
    private final JTree navigator;
    private final JTabbedPane editors;

    public ApplicationFrame() {
        try {
            this.workspace = new Workspace();
            this.navigator = new JTree();
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
        pane.add(new JScrollPane(navigator));
        pane.add(editors);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new MigLayout("insets dialog", "[grow,fill]", "[grow,fill]"));
        contentPane.add(pane);

        loadProjects();
    }

    private void initializeEditorsPane() {
        editors.setBorder(new FlatBorder());
        editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close");
        editors.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, (IntConsumer) editors::removeTabAt);
        editors.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        editors.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        editors.addChangeListener(e -> setTitle(getApplicationTitle()));
        editors.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                final int index = editors.indexAtLocation(e.getX(), e.getY());
                if (SwingUtilities.isRightMouseButton(e) && index >= 0) {
                    final EditorPane editor = (EditorPane) editors.getComponentAt(index);
                    final JPopupMenu menu = new JPopupMenu();
                    menu.add(new JMenuItem(new AbstractAction("Close") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editors.remove(editor);
                        }
                    }));
                    menu.add(new JMenuItem(new AbstractAction("Close All") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editors.removeAll();
                        }
                    }));
                    menu.addSeparator();
                    menu.add(new JMenuItem(new AbstractAction("Show in Navigator") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            final TreePath path = UIUtils.getPath(editor.getNode());
                            navigator.setSelectionPath(path);
                            navigator.scrollPathToVisible(path);
                            navigator.requestFocusInWindow();
                        }
                    }));
                    menu.show(editors, e.getX(), e.getY());
                }
            }
        });
    }

    private void initializeNavigatorPane() {
        final DefaultTreeModel model = new DefaultTreeModel(null);
        model.setRoot(new NavigatorWorkspaceNode(workspace, model));

        navigator.setModel(model);
        navigator.setRootVisible(false);
        navigator.setToggleClickCount(0);
        navigator.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                final Object component = event.getPath().getLastPathComponent();
                if (component instanceof NavigatorLazyNode node) {
                    node.loadChildren(navigator, e -> { /* currently unused */ });
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
    }

    private void loadProjects() {
        workspace.addProject(new Project(
            Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/ds.exe"),
            Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/data"),
            getResourcePath("ds_types.json"),
            getResourcePath("ds_archives.json"),
            Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/oo2core_7_win64.dll"),
            GameType.DS
        ));

        workspace.addProject(new Project(
            Path.of("E:/SteamLibrary/steamapps/common/Horizon Zero Dawn/HorizonZeroDawn.exe"),
            Path.of("E:/SteamLibrary/steamapps/common/Horizon Zero Dawn/Packed_DX12"),
            getResourcePath("hzd_types.json"),
            null,
            Path.of("E:/SteamLibrary/steamapps/common/Horizon Zero Dawn/oo2core_3_win64.dll"),
            GameType.HZD
        ));
    }

    @NotNull
    private Path getResourcePath(@NotNull String name) {
        try {
            return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(name)).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid resource URI", e);
        }
    }

    @NotNull
    private Project getProject(@NotNull NavigatorNode node) {
        final NavigatorProjectNode project = getParentNode(node, NavigatorProjectNode.class);
        if (project == null) {
            throw new IllegalArgumentException("Incorrect node hierarchy");
        }
        return project.getProject();
    }

    @Nullable
    private <T extends NavigatorNode> T getParentNode(@NotNull NavigatorNode node, @NotNull Class<T> clazz) {
        for (NavigatorNode current = node; current != null; current = current.getParent()) {
            if (clazz.isInstance(current)) {
                return clazz.cast(current);
            }
        }
        return null;
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

        final EditorPane pane = new EditorPane(getProject(node), node);
        editors.addTab(node.getLabel(), pane);
        editors.setSelectedComponent(pane);
        editors.requestFocusInWindow();
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
            workspace.close();
        } catch (IOException e) {
            log.error("Error closing workspace", e);
        }

        super.dispose();
    }
}
