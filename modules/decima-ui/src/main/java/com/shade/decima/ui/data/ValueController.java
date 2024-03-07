package com.shade.decima.ui.data;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface ValueController<T> {
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
    RTTICoreFile getCoreFile();

    @NotNull
    T getValue();
}
