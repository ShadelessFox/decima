package com.shade.decima.ui.data.editors;

import com.shade.decima.model.rtti.types.RTTITypeNumber;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.handlers.NumberValueHandler;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

public class NumberValueEditor extends BaseValueEditor<Number, JTextComponent> {
    private static final BigInteger INT128_MIN = BigInteger.ZERO;
    private static final BigInteger INT128_MAX = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);

    private static final Map<Class<? extends Number>, Converter> CONVERTERS = Map.of(
        byte.class, (value, signed) -> signed ? Byte.parseByte(value) : checkRange(Integer.parseUnsignedInt(value), 0, 255).byteValue(),
        short.class, (value, signed) -> signed ? Short.parseShort(value) : checkRange(Integer.parseUnsignedInt(value), 0, 65535).shortValue(),
        int.class, (value, signed) -> signed ? Integer.parseInt(value) : Integer.parseUnsignedInt(value),
        long.class, (value, signed) -> signed ? Long.parseLong(value) : Long.parseUnsignedLong(value),
        BigInteger.class, (value, signed) -> signed ? new BigInteger(value) : checkRange(new BigInteger(value), INT128_MIN, INT128_MAX),
        float.class, (value, signed) -> Float.parseFloat(value),
        double.class, (value, signed) -> Double.parseDouble(value)
    );

    public NumberValueEditor(@NotNull ValueController<Number> controller) {
        super(controller);
    }

    @NotNull
    @Override
    protected JTextComponent createComponentImpl() {
        return new JTextField(null, 8);
    }

    @Nullable
    @Override
    protected InputValidator createInputValidator(@NotNull JTextComponent component) {
        return new EditorInputValidator(component);
    }

    @Override
    public void setEditorValue(@NotNull Number value) {
        component.setText(NumberValueHandler.toString((RTTITypeNumber<?>) controller.getValueType(), value));
    }

    @NotNull
    @Override
    public Number getEditorValue() {
        final RTTITypeNumber<Number> type = (RTTITypeNumber<Number>) controller.getValueType();
        final Converter converter = Objects.requireNonNull(CONVERTERS.get(type.getInstanceType()), "Couldn't find converter for type " + type);
        return converter.convert(component.getText(), type.isSigned());
    }

    @Override
    public void addActionListener(@NotNull ActionListener listener) {
        if (component instanceof JTextField field) {
            field.addActionListener(listener);
        }
    }

    @Override
    public void removeActionListener(@NotNull ActionListener listener) {
        if (component instanceof JTextField field) {
            field.removeActionListener(listener);
        }
    }

    @NotNull
    private static <T extends Number & Comparable<? super T>> T checkRange(@NotNull T value, @NotNull T min, @NotNull T max) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new NumberFormatException("Value out of range, should be in between " + min + " and " + max + " inclusive");
        }

        return value;
    }

    private interface Converter {
        @NotNull
        Number convert(@NotNull String value, boolean signed);
    }
}
