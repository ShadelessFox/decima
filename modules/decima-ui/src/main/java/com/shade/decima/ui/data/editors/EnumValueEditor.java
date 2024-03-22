package com.shade.decima.ui.data.editors;

import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.controls.plaf.ThinFlatComboBoxUI;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;

public class EnumValueEditor extends BaseValueEditor<RTTITypeEnum.Constant, JComboBox<RTTITypeEnum.Constant>> {
    public EnumValueEditor(@NotNull MutableValueController<RTTITypeEnum.Constant> controller) {
        super(controller);
    }

    @NotNull
    @Override
    protected JComboBox<RTTITypeEnum.Constant> createComponentImpl() {
        final RTTITypeEnum type = (RTTITypeEnum) controller.getValueType();
        final RTTITypeEnum.Constant[] constants = Arrays.stream(type.values())
            .sorted(Comparator.comparing(RTTITypeEnum.Constant::value))
            .toArray(RTTITypeEnum.Constant[]::new);

        final JComboBox<RTTITypeEnum.Constant> combo = new JComboBox<>(constants);
        combo.setUI(new ThinFlatComboBoxUI());
        combo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends RTTITypeEnum.Constant> list, @NotNull RTTITypeEnum.Constant value, int index, boolean selected, boolean focused) {
                append(value.name() + " ", TextAttributes.REGULAR_ATTRIBUTES);
                append(String.valueOf(value.value()), TextAttributes.GRAYED_SMALL_ATTRIBUTES);
            }
        });

        return combo;
    }

    @Override
    public void setEditorValue(@NotNull RTTITypeEnum.Constant value) {
        component.setSelectedItem(value);
    }

    @NotNull
    @Override
    public RTTITypeEnum.Constant getEditorValue() {
        return component.getItemAt(component.getSelectedIndex());
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
