package com.shade.platform.ui.settings.impl;

import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.dialogs.BaseEditDialog;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.platform.ui.settings.SettingsRegistry;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SettingsDialog extends BaseEditDialog {
    private final Map<SettingsPageRegistration, PageInfo> pageCache = new HashMap<>();

    private SettingsTree tree;
    private JLabel activePageTitleLabel;
    private JLabel activePageRevertLabel;
    private JPanel activePagePanel;
    private PageInfo activePageInfo;

    private static String lastSelectedPagePath;

    public SettingsDialog() {
        super("Settings", true);
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        activePageTitleLabel = UIUtils.createBoldLabel();

        activePageRevertLabel = UIUtils.createBoldLabel();
        activePageRevertLabel.setVisible(false);
        activePageRevertLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        activePageRevertLabel.setText("<html><a href=\"#\">Reset</a></html>");
        activePageRevertLabel.setToolTipText("Rollback changes for this page");
        activePageRevertLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                activePageInfo.page.reset();
            }
        });

        activePagePanel = new JPanel();
        activePagePanel.setLayout(new MigLayout("ins panel", "grow,fill", "grow,fill"));

        tree = new SettingsTree(this);
        tree.addTreeSelectionListener(e -> {
            final TreePath path = e.getNewLeadSelectionPath();
            if (path == null) {
                return;
            }
            final Object component = path.getLastPathComponent();
            if (component instanceof SettingsTreeNodePage node) {
                setActivePage(node.getPage(), node.getMetadata());
            }
        });

        if (lastSelectedPagePath != null) {
            tree.getModel()
                .findPageNode(new VoidProgressMonitor(), lastSelectedPagePath.split("/"))
                .whenComplete((node, exception) -> {
                    if (exception != null) {
                        UIUtils.showErrorDialog(exception);
                        return;
                    }

                    if (node != null) {
                        final TreePath path = tree.getModel().getTreePathToRoot(node);
                        tree.setSelectionPath(path);
                        tree.scrollPathToVisible(path);
                    }
                });
        } else {
            tree.setSelectionRow(0);
        }

        final JScrollPane navigatorPane = UIUtils.createBorderlessScrollPane(tree);
        navigatorPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navigatorPane.setPreferredSize(new Dimension(200, 0));

        final JPanel pageHeader = new JPanel();
        pageHeader.setLayout(new MigLayout("ins panel", "[grow,fill][]"));
        pageHeader.add(activePageTitleLabel);
        pageHeader.add(activePageRevertLabel);

        final JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIColor.SHADOW));
        contentPane.setLayout(new BorderLayout(0, 0));
        contentPane.add(pageHeader, BorderLayout.NORTH);
        contentPane.add(activePagePanel, BorderLayout.CENTER);

        final JPanel root = new JPanel();
        root.setLayout(new BorderLayout());
        root.add(navigatorPane, BorderLayout.WEST);
        root.add(contentPane, BorderLayout.CENTER);

        return root;
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_OK, BUTTON_CANCEL, BUTTON_APPLY};
    }

    @Nullable
    @Override
    protected Dimension getMinimumSize() {
        return new Dimension(900, 550);
    }

    @Override
    protected boolean isComplete() {
        return pageCache.values().stream().allMatch(PageInfo::isComplete);
    }

    @Override
    protected boolean isButtonEnabled(@NotNull ButtonDescriptor descriptor) {
        if (descriptor == BUTTON_OK) {
            return isComplete();
        } else if (descriptor == BUTTON_APPLY) {
            return isComplete() && pageCache.values().stream().anyMatch(PageInfo::isModified);
        } else {
            return true;
        }
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        if (descriptor == BUTTON_OK || descriptor == BUTTON_APPLY) {
            applyChanges();
        }

        if (descriptor == BUTTON_APPLY) {
            return;
        }

        super.buttonPressed(descriptor);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);

        if (activePageInfo != null) {
            activePageRevertLabel.setVisible(activePageInfo.isModified());
        }
    }

    public boolean isModified(@NotNull SettingsPageRegistration metadata) {
        final PageInfo info = pageCache.get(metadata);
        return info != null && info.isModified();
    }

    public boolean isComplete(@NotNull SettingsPageRegistration metadata) {
        final PageInfo info = pageCache.get(metadata);
        return info == null || info.isComplete();
    }

    private void applyChanges() {
        for (PageInfo info : pageCache.values()) {
            if (info.isModified()) {
                info.page.apply();
            }
        }

        propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
    }

    private void setActivePage(@NotNull SettingsPage page, @NotNull SettingsPageRegistration metadata) {
        if (activePageInfo != null && activePageInfo.metadata == metadata) {
            return;
        }

        activePageInfo = pageCache.computeIfAbsent(metadata, m -> new PageInfo(page, m));
        activePageTitleLabel.setText(activePageInfo.getPageTitle());
        activePageRevertLabel.setVisible(activePageInfo.isModified());
        activePagePanel.removeAll();
        activePagePanel.add(activePageInfo.component);
        activePagePanel.revalidate();
        activePagePanel.repaint();

        lastSelectedPagePath = activePageInfo.getPagePath();
    }

    private class PageInfo {
        private final SettingsPage page;
        private final SettingsPageRegistration metadata;
        private final JComponent component;

        public PageInfo(@NotNull SettingsPage page, @NotNull SettingsPageRegistration metadata) {
            this.page = page;
            this.metadata = metadata;
            this.component = page.createComponent(SettingsDialog.this);

            page.reset();
        }

        public boolean isComplete() {
            return page.isComplete();
        }

        public boolean isModified() {
            return page.isModified();
        }

        @NotNull
        private String getPageTitle() {
            return Arrays.stream(getPathToRoot(metadata, 0))
                .map(SettingsPageRegistration::name)
                .collect(Collectors.joining(" \u203a "));
        }

        @NotNull
        private String getPagePath() {
            return Arrays.stream(getPathToRoot(metadata, 0))
                .map(SettingsPageRegistration::id)
                .collect(Collectors.joining("/"));
        }

        @NotNull
        private static SettingsPageRegistration[] getPathToRoot(@NotNull SettingsPageRegistration meta, int depth) {
            final SettingsPageRegistration[] elements;

            if (meta.parent().isEmpty()) {
                elements = new SettingsPageRegistration[depth + 1];
            } else {
                final var page = SettingsRegistry.getInstance().getPageById(meta.parent());
                elements = getPathToRoot(page.metadata(), depth + 1);
            }

            elements[elements.length - depth - 1] = meta;

            return elements;
        }
    }
}
