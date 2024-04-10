package com.shade.decima.ui.data.editors;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
                setEditorValue(RTTIUtils.randomUUID((RTTIClass) controller.getValueType()));
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
        return RTTIUtils.uuidFromString((RTTIClass) controller.getValueType(), component.getText());
    }

    @Override
    public void addActionListener(@NotNull ActionListener listener) {
        component.addActionListener(listener);
    }

    @Override
    public void removeActionListener(@NotNull ActionListener listener) {
        component.removeActionListener(listener);
    }
}
