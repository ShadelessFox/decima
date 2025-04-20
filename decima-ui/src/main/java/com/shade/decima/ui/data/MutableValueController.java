package com.shade.decima.ui.data;

import com.shade.util.NotNull;

public interface MutableValueController<T> extends ValueController<T> {
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

    void setValue(@NotNull T value);
}
