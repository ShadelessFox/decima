package com.shade.decima.ui.data.editors;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ThreadLocalRandom;

public class GGUUIDValueEditor extends BaseValueEditor<RTTIObject, JTextField> {
    public GGUUIDValueEditor(@NotNull MutableValueController<RTTIObject> controller) {
        super(controller);
    }

    @NotNull
    @Override
    protected JTextField createComponentImpl() {
        final JToolBar toolBar = new JToolBar();
        toolBar.add(new AbstractAction(null, UIManager.getIcon("Action.refreshIcon")) {
            {
                putValue(SHORT_DESCRIPTION, "Generate new value");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                setEditorValue(randomUUID((RTTIClass) controller.getValueType()));
            }
        });

        final JTextField field = new JTextField(null, 24);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);

        return field;
    }

    @Nullable
    @Override
    protected InputValidator createInputValidator(@NotNull JTextField component) {
        return new EditorInputValidator(component);
    }

    @Override
    public void setEditorValue(@NotNull RTTIObject value) {
        component.setText("{%s}".formatted(RTTIUtils.uuidToString(value)));
    }

    @NotNull
    @Override
    public RTTIObject getEditorValue() {
        return RTTIUtils.uuidFromString((RTTITypeClass) controller.getValueType(), component.getText());
    }

    @Override
    public void addActionListener(@NotNull ActionListener listener) {
        component.addActionListener(listener);
    }

    @Override
    public void removeActionListener(@NotNull ActionListener listener) {
        component.removeActionListener(listener);
    }

    @NotNull
    private static RTTIObject randomUUID(@NotNull RTTIClass type) {
        final byte[] bytes = new byte[16];
        ThreadLocalRandom.current().nextBytes(bytes);

        final RTTIObject object = type.create();
        object.set("Data0", bytes[0]);
        object.set("Data1", bytes[1]);
        object.set("Data2", bytes[2]);
        object.set("Data3", bytes[3]);
        object.set("Data4", bytes[4]);
        object.set("Data5", bytes[5]);
        object.set("Data6", bytes[6]);
        object.set("Data7", bytes[7]);
        object.set("Data8", bytes[8]);
        object.set("Data9", bytes[9]);
        object.set("Data10", bytes[10]);
        object.set("Data11", bytes[11]);
        object.set("Data12", bytes[12]);
        object.set("Data13", bytes[13]);
        object.set("Data14", bytes[14]);
        object.set("Data15", bytes[15]);

        return object;
    }
}
