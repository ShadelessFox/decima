package com.shade.decima.ui.data.editors;

import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionListener;

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

        UIUtils.addOpenAction(refPathText, e -> { /* TODO */ });
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

        final String path = refPathText.getText();
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

    @Override
    public void addActionListener(@NotNull ActionListener listener) {

    }

    @Override
    public void removeActionListener(@NotNull ActionListener listener) {

    }
}
