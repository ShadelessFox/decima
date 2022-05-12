package com.shade.decima.ui;

import com.formdev.flatlaf.ui.FlatBorder;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.action.Actions;
import com.shade.decima.ui.editor.EditorPane;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.dnd.FileTransferHandler;
import com.shade.decima.ui.navigator.impl.NavigatorWorkspaceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class ApplicationFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(ApplicationFrame.class);

    private final Workspace workspace;
    private final NavigatorTree navigator;
    private final EditorsPane editors;

    public ApplicationFrame() {
        try {
            this.workspace = new Workspace();
            this.navigator = new NavigatorTree(new NavigatorWorkspaceNode(workspace));
            this.editors = new EditorsPane();

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

        navigator.setBorder(null);
        editors.setBorder(null);

        final JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pane.add(navigator);
        pane.add(editors);

        getContentPane().add(pane);
    }

    private void initializeEditorsPane() {
        editors.setBorder(new FlatBorder());
        editors.addPropertyChangeListener("activeEditor", e -> setTitle(getApplicationTitle()));
    }

    private void initializeNavigatorPane() {
        final JTree tree = navigator.getTree();
        tree.setRootVisible(false);
        tree.setTransferHandler(new FileTransferHandler());
        tree.setDropTarget(null);
        tree.setDragEnabled(true);
    }

    @NotNull
    public NavigatorTree getNavigator() {
        return navigator;
    }

    @NotNull
    public EditorsPane getEditorsPane() {
        return editors;
    }

    @NotNull
    private String getApplicationTitle() {
        final EditorPane activeEditor = editors.getActiveEditor();
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
