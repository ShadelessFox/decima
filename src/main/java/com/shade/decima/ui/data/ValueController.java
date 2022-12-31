package com.shade.decima.ui.data;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.util.NotNull;

public interface ValueController<T> {
    enum EditType {
        /**
         * An inline editor that appears right within the parent control (e.g. {@link javax.swing.JTree}).
         */
        INLINE,

        /**
         * A standalone editor that appears in a model dialog.
         */
        DIALOG
    }

    @NotNull
    EditType getEditType();

    @NotNull
    ValueManager<T> getValueManager();

    @NotNull
    RTTIType<T> getValueType();

    @NotNull
    String getValueLabel();

    @NotNull
    Project getProject();

    @NotNull
    T getValue();

    void setValue(@NotNull T value);
}
