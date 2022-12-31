package com.shade.decima.ui.data.editors;

import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.controls.validation.Validation;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.BaseEditDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class ReferenceValueEditor implements ValueEditor<RTTIReference> {
    private final ValueController<RTTIReference> controller;
    private JTextField refPathText;
    private JTextField refUuidText;
    private JComboBox<RTTIReference.Kind> refKindCombo;

    public ReferenceValueEditor(@NotNull ValueController<RTTIReference> controller) {
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
            final FileEditorInput input = (FileEditorInput) controller.getEditor().getInput();
            final NavigatorProjectNode root = input.getNode().getParentOfType(NavigatorProjectNode.class);
            final Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            final PathPickerDialog dialog = new PathPickerDialog("Choose target file", root);

            if (dialog.showDialog(window) == BaseDialog.BUTTON_OK) {
                refPathText.setText(dialog.getPath());
            }
        });

        UIUtils.addOpenAction(refUuidText, e -> { /* TODO */ });

        return panel;
    }

    @Override
    public void setEditorValue(@NotNull RTTIReference value) {
        if (value instanceof RTTIReference.External ref) {
            refPathText.setText(ref.path());
            refUuidText.setText(GGUUIDValueHandler.INSTANCE.getString(ref.uuid().type(), ref.uuid()));
            refKindCombo.setSelectedItem(ref.kind());
        } else if (value instanceof RTTIReference.Internal ref) {
            refUuidText.setText(GGUUIDValueHandler.INSTANCE.getString(ref.uuid().type(), ref.uuid()));
            refKindCombo.setSelectedItem(ref.kind());
        }
    }

    @NotNull
    @Override
    public RTTIReference getEditorValue() {
        final RTTITypeClass GGUUID = controller.getProject().getTypeRegistry().find("GGUUID");

        final String path = PackfileBase.getNormalizedPath(IOUtils.getBasename(refPathText.getText()), false);
        final String uuid = refUuidText.getText();
        final RTTIReference.Kind kind = refKindCombo.getItemAt(refKindCombo.getSelectedIndex());

        if (uuid.isEmpty()) {
            return RTTIReference.NONE;
        } else if (path.isEmpty()) {
            return new RTTIReference.Internal(kind, GGUUIDValueEditor.fromString(GGUUID, uuid));
        } else {
            return new RTTIReference.External(kind, GGUUIDValueEditor.fromString(GGUUID, uuid), path);
        }
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

                if (component instanceof NavigatorFileNode) {
                    return Validation.ok();
                } else {
                    return Validation.error("The selected not is not a file");
                }
            }
        }
    }
}
