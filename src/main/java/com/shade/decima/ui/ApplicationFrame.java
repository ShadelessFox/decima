package com.shade.decima.ui;

import com.shade.decima.Project;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.handlers.ValueCollectionHandler;
import com.shade.decima.ui.handlers.ValueHandler;
import com.shade.decima.ui.handlers.ValueHandlerProvider;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorWorkspaceNode;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.regex.Pattern;

public class ApplicationFrame extends JFrame {
    private final Project project;
    private final JTree properties;

    public ApplicationFrame() {
        try {
            this.project = new Project(Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/ds.exe"));
            this.properties = new JTree((TreeModel) null);
            this.properties.setCellRenderer(new StyledListCellRenderer());

            setTitle("Decima Explorer");

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
        pane.add(new JScrollPane(properties));

        final Container contentPane = getContentPane();
        contentPane.setLayout(new MigLayout("insets dialog", "[grow,fill]", "[grow,fill]"));
        contentPane.add(pane);
    }

    private boolean navigateFromPath(@Nullable TreePath path) {
        if (path != null) {
            final Object component = path.getLastPathComponent();

            if (component instanceof NavigatorFileNode file) {
                navigate(file);
                return true;
            }
        }

        return false;
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

    public void navigate(@NotNull NavigatorFileNode node) {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root", true);

        try {
            for (RTTIObject object : project.getArchiveManager().readFileObjects(project.getCompressor(), node.getFile())) {
                append(root, object.getType(), object);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        properties.setModel(new DefaultTreeModel(root));
        properties.expandPath(new TreePath(root.getPath()));
    }

    public void append(@NotNull DefaultMutableTreeNode root, @NotNull RTTIType<?> type, @NotNull Object value) {
        append(root, RTTITypeRegistry.getFullTypeName(type), type, value);
    }

    @SuppressWarnings("unchecked")
    public void append(@NotNull DefaultMutableTreeNode root, @Nullable String name, @NotNull RTTIType<?> type, @NotNull Object value) {
        final ValueHandler handler = ValueHandlerProvider.getValueHandler(type);
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        final StringBuilder sb = new StringBuilder("<html>");
        final String inline = handler.getInlineValue(type, value);

        if (name != null) {
            sb.append("<font color=#7f0000>%s</font> = ".formatted(escapeLabelName(name)));
        }

        if (inline != null) {
            sb.append(inline);
        } else {
            sb.append("<font color=gray>{%s}</font>".formatted(escapeLabelName(RTTITypeRegistry.getFullTypeName(type))));
        }

        if (handler instanceof ValueCollectionHandler) {
            final ValueCollectionHandler<Object, Object> container = (ValueCollectionHandler<Object, Object>) handler;
            final Collection<?> children = container.getChildren(type, value);

            if (type.getKind() == RTTIType.Kind.CONTAINER) {
                sb.append(" size = ").append(children.size());
            }

            for (Object child : children) {
                append(
                    node,
                    container.getChildName(type, value, child),
                    container.getChildType(type, value, child),
                    container.getChildValue(type, value, child)
                );
            }
        }

        sb.append("</html>");

        node.setUserObject(sb.toString());

        root.add(node);
    }

    @NotNull
    private static String escapeLabelName(@NotNull String label) {
        return label.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @NotNull
    private static String unescapeLabelName(@NotNull String label) {
        return label.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    private static class StyledListCellRenderer extends DefaultTreeCellRenderer {
        private static final Pattern TAG_PATTERN = Pattern.compile("<.*?>");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value != null && selected) {
                value = unescapeLabelName(TAG_PATTERN.matcher(value.toString()).replaceAll(""));
            }

            return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    }

}
