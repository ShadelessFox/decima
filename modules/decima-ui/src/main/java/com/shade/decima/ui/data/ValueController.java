package com.shade.decima.ui.data;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface ValueController<T> {
    enum EditType {
        /**
         * An inline editor that appears right within the parent control (e.g. {@link javax.swing.JTree}).
         */
        INLINE,

        /**
         * A standalone editor that appears in a modal dialog.
         */
        DIALOG
    }

    @NotNull
    EditType getEditType();

    @NotNull
    RTTIType<T> getValueType();

    @Nullable
    RTTIPath getValuePath();

    @NotNull
    String getValueLabel();

    @NotNull
    Editor getEditor();

    @NotNull
    Project getProject();

    @NotNull
    CoreBinary getBinary();

    @NotNull
    T getValue();

    void setValue(@NotNull T value);
}
