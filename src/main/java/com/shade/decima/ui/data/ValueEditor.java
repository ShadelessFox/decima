package com.shade.decima.ui.data;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;

public interface ValueEditor {
    @NotNull
    JComponent createComponent(@NotNull RTTIType<?> type);

    void setEditorValue(@NotNull JComponent component, @NotNull RTTIType<?> type, @NotNull Object value);

    @Nullable
    Object getEditorValue(@NotNull JComponent component, @NotNull RTTIType<?> type);

    void addActionListener(@NotNull JComponent component, @NotNull ActionListener listener);

    void removeActionListener(@NotNull JComponent component, @NotNull ActionListener listener);
}
