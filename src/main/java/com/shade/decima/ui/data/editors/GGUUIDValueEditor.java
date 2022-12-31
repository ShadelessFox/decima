package com.shade.decima.ui.data.editors;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.UUID;

public class GGUUIDValueEditor extends BaseValueEditor<RTTIObject, JTextField> {
    public GGUUIDValueEditor(@NotNull ValueController<RTTIObject> controller) {
        super(controller);
    }

    @NotNull
    @Override
    protected JTextField createComponentImpl() {
        return new JTextField();
    }

    @Override
    public void setEditorValue(@NotNull RTTIObject value) {
        component.setText("{%s}".formatted(GGUUIDValueHandler.INSTANCE.getString(value.type(), value)));
    }

    @NotNull
    @Override
    public RTTIObject getEditorValue() {
        return fromString((RTTITypeClass) controller.getValueType(), component.getText());
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
    private static RTTIObject fromString(@NotNull RTTITypeClass type, @NotNull String text) {
        final UUID uuid;

        if (text.startsWith("{") && text.endsWith("}")) {
            uuid = UUID.fromString(text.substring(1, text.length() - 1));
        } else {
            uuid = UUID.fromString(text);
        }

        final long msb = uuid.getMostSignificantBits();
        final long lsb = uuid.getLeastSignificantBits();
        final RTTIObject object = type.instantiate();

        object.set("Data3", (byte) (msb >>> 56));
        object.set("Data2", (byte) (msb >>> 48));
        object.set("Data1", (byte) (msb >>> 40));
        object.set("Data0", (byte) (msb >>> 32));
        object.set("Data5", (byte) (msb >>> 24));
        object.set("Data4", (byte) (msb >>> 16));
        object.set("Data7", (byte) (msb >>> 8));
        object.set("Data6", (byte) (msb));
        object.set("Data8", (byte) (lsb >>> 56));
        object.set("Data9", (byte) (lsb >>> 48));
        object.set("Data10", (byte) (lsb >>> 40));
        object.set("Data11", (byte) (lsb >>> 32));
        object.set("Data12", (byte) (lsb >>> 24));
        object.set("Data13", (byte) (lsb >>> 16));
        object.set("Data14", (byte) (lsb >>> 8));
        object.set("Data15", (byte) (lsb));

        return object;
    }
}
