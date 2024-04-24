package com.shade.decima.ui.data.editors;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.*;
import com.shade.decima.model.rtti.RTTICoreFileReader.LoggingErrorHandlingStrategy;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.RTTITypeReference;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.controls.validation.Validation;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.BaseEditDialog;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public class ReferenceValueEditor implements ValueEditor<RTTIReference> {
    private final MutableValueController<RTTIReference> controller;
    private JTextField refPathText;
    private JTextField refUuidText;
    private JComboBox<RTTIReference.Kind> refKindCombo;

    public ReferenceValueEditor(@NotNull MutableValueController<RTTIReference> controller) {
        this.controller = controller;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        final JLabel refPathLabel = Mnemonic.resolve(new JLabel("&Path: "));
        refPathLabel.setToolTipText("A core file this reference points to.\nLeave empty to use the current file.");
        refPathLabel.setLabelFor(refPathText = new JTextField());

        final JLabel refUuidLabel = Mnemonic.resolve(new JLabel("&Entry: "));
        refUuidLabel.setToolTipText("A core file entry this reference points to.\nLeave empty to denote an empty reference.");
        refUuidLabel.setLabelFor(refUuidText = new JTextField());

        final JLabel refKindLabel = Mnemonic.resolve(new JLabel("&Kind: "));
        refKindLabel.setToolTipText("A kind of this reference.");
        refKindLabel.setLabelFor(refKindCombo = new JComboBox<>(RTTIReference.Kind.values()));

        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0", "[fill][grow,fill,350lp]"));

        panel.add(refPathLabel);
        panel.add(refPathText, "wrap");

        panel.add(refUuidLabel);
        panel.add(refUuidText, "wrap");

        panel.add(refKindLabel);
        panel.add(refKindCombo, "wrap");

        UIUtils.addOpenAction(refPathText, e -> {
            final NodeEditorInput input = (NodeEditorInput) controller.getEditor().getInput();
            final NavigatorProjectNode root = input.getNode().getParentOfType(NavigatorProjectNode.class);
            final Window window = JOptionPane.getRootFrame();
            final PathPickerDialog dialog = new PathPickerDialog("Choose target file", root);

            if (dialog.showDialog(window) == BaseDialog.BUTTON_OK) {
                refPathText.setText(dialog.getPath());
            }
        });

        UIUtils.addOpenAction(refUuidText, e -> {
            final Window window = JOptionPane.getRootFrame();
            final String path = getPath();
            final EntryPickerDialog dialog = new EntryPickerDialog("Choose target entry", window, controller.getProject(), path);

            if (dialog.file != null && dialog.showDialog(window) == BaseDialog.BUTTON_OK) {
                final RTTIObject uuid = dialog.getUUID();
                refUuidText.setText(RTTIUtils.uuidToString(uuid));
            }
        });

        return panel;
    }

    @Override
    public void setEditorValue(@NotNull RTTIReference value) {
        if (value instanceof RTTIReference.External ref) {
            refPathText.setText(ref.path());
            refUuidText.setText(RTTIUtils.uuidToString(ref.uuid()));
            refKindCombo.setSelectedItem(ref.kind());
        } else if (value instanceof RTTIReference.Internal ref) {
            refUuidText.setText(RTTIUtils.uuidToString(ref.uuid()));
            refKindCombo.setSelectedItem(ref.kind());
        }

        refPathText.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, getCurrentPath());
    }

    @NotNull
    @Override
    public RTTIReference getEditorValue() {
        final RTTIClass GGUUID = controller.getProject().getRTTIFactory().find("GGUUID");

        final String path = getPath();
        final String uuid = refUuidText.getText();
        final RTTIReference.Kind kind = refKindCombo.getItemAt(refKindCombo.getSelectedIndex());

        if (uuid.isEmpty()) {
            return RTTIReference.NONE;
        } else if (path.isEmpty() || path.equals(getCurrentPath())) {
            return new RTTIReference.Internal(kind, RTTIUtils.uuidFromString(GGUUID, uuid));
        } else {
            return new RTTIReference.External(kind, RTTIUtils.uuidFromString(GGUUID, uuid), IOUtils.getBasename(path));
        }
    }

    @NotNull
    private String getPath() {
        final String path = refPathText.getText();

        if (path.isEmpty()) {
            return getCurrentPath();
        } else {
            return Packfile.getNormalizedPath(path);
        }
    }

    @NotNull
    private String getCurrentPath() {
        final NodeEditorInput input = (NodeEditorInput) controller.getEditor().getInput();
        final String path = input.getNode().getPath().full();
        return Packfile.getNormalizedPath(path);
    }

    private static class PathPickerDialog extends BaseEditDialog {
        private final NavigatorNode root;
        private NavigatorTree tree;

        public PathPickerDialog(@NotNull String title, @NotNull NavigatorNode root) {
            super(title);
            this.root = root;
        }

        @NotNull
        public String getPath() {
            return ((NavigatorFileNode) tree.getLastSelectedPathComponent()).getPath().full();
        }

        @NotNull
        @Override
        protected JComponent createContentsPane() {
            final JScrollPane pane = new JScrollPane(tree = new NavigatorTree(root));
            UIUtils.installInputValidator(tree, new SelectionValidator(tree, pane), this);
            return pane;
        }

        @Nullable
        @Override
        protected Dimension getMinimumSize() {
            return new Dimension(350, 450);
        }

        @Override
        protected boolean isComplete() {
            return UIUtils.isValid(tree);
        }

        private static class SelectionValidator extends InputValidator {
            public SelectionValidator(@NotNull JComponent component, @NotNull JComponent overlay) {
                super(component, overlay);
            }

            @NotNull
            @Override
            protected Validation validate(@NotNull JComponent input) {
                final JTree tree = (JTree) input;
                final Object component = tree.getLastSelectedPathComponent();

                if (component instanceof NavigatorFileNode node && node.getExtension().equals("core")) {
                    return Validation.ok();
                } else {
                    return Validation.error("The selected not is not a file");
                }
            }
        }
    }

    private class EntryPickerDialog extends BaseEditDialog {
        private final RTTICoreFile file;
        private JList<RTTIObject> list;

        public EntryPickerDialog(@NotNull String title, @NotNull Window window, @NotNull Project project, @NotNull String path) {
            super(title);

            final Optional<RTTICoreFile> result = ProgressDialog.showProgressDialog(window, "Enumerate entries", monitor -> {
                try (ProgressMonitor.IndeterminateTask ignored = monitor.begin("Read core file")) {
                    try {
                        return project.getCoreFileReader().read(project.getPackfileManager().getFile(path), LoggingErrorHandlingStrategy.getInstance());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });

            this.file = result.orElse(null);
        }

        @NotNull
        public RTTIObject getUUID() {
            return list.getSelectedValue().uuid();
        }

        @NotNull
        @Override
        protected JComponent createContentsPane() {
            final RTTIType<?> parent = ((RTTITypeReference) controller.getValueType()).getComponentType();
            final RTTIObject[] entries = file.objects().stream()
                .filter(entry -> descendsFrom(parent, entry.type()))
                .toArray(RTTIObject[]::new);

            list = new JList<>(entries);
            list.setCellRenderer(new ColoredListCellRenderer<>() {
                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends RTTIObject> list, @NotNull RTTIObject value, int index, boolean selected, boolean focused) {
                    append("[%d] ".formatted(index), TextAttributes.GRAYED_ATTRIBUTES);
                    append(value.type().getFullTypeName(), CommonTextAttributes.IDENTIFIER_ATTRIBUTES);
                    append(" ", TextAttributes.REGULAR_ATTRIBUTES);
                    append(RTTIUtils.uuidToString(value.uuid()), TextAttributes.REGULAR_ATTRIBUTES);
                }
            });

            final JScrollPane pane = new JScrollPane(list);
            UIUtils.installInputValidator(list, new SelectionValidator(list, pane), this);
            return pane;
        }

        @Nullable
        @Override
        protected Dimension getMinimumSize() {
            return new Dimension(350, 450);
        }

        @Override
        protected boolean isComplete() {
            return UIUtils.isValid(list);
        }

        private static boolean descendsFrom(@NotNull RTTIType<?> parent, @NotNull RTTIType<?> child) {
            if (parent instanceof RTTIClass p && child instanceof RTTIClass c && c.isInstanceOf(p)) {
                return true;
            }
            if (parent instanceof RTTITypeParameterized<?, ?> p) {
                return descendsFrom(p.getComponentType(), child);
            }
            return false;
        }

        private static class SelectionValidator extends InputValidator {
            public SelectionValidator(@NotNull JComponent component, @NotNull JComponent overlay) {
                super(component, overlay);
            }

            @NotNull
            @Override
            protected Validation validate(@NotNull JComponent input) {
                final JList<?> list = (JList<?>) input;

                if (list.isSelectionEmpty()) {
                    return Validation.error("No entry selected");
                } else {
                    return Validation.ok();
                }
            }
        }
    }
}
