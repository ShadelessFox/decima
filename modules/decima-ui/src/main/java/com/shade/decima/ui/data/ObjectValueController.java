package com.shade.decima.ui.data;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public record ObjectValueController(@NotNull ValueController<?> delegate, @NotNull RTTICoreFile file, @NotNull RTTIObject object) implements ValueController<RTTIObject> {
    @NotNull
    @Override
    public EditType getEditType() {
        return EditType.INLINE;
    }

    @NotNull
    @Override
    public RTTIType<RTTIObject> getValueType() {
        return object.type();
    }

    @Nullable
    @Override
    public RTTIPath getValuePath() {
        return null;
    }

    @NotNull
    @Override
    public String getValueLabel() {
        return object.type().getFullTypeName();
    }

    @NotNull
    @Override
    public Editor getEditor() {
        return delegate.getEditor();
    }

    @NotNull
    @Override
    public Project getProject() {
        return delegate.getProject();
    }

    @NotNull
    @Override
    public RTTICoreFile getCoreFile() {
        return file;
    }

    @NotNull
    @Override
    public RTTIObject getValue() {
        return object;
    }

    @Override
    public void setValue(@NotNull RTTIObject value) {
        // not implemented
    }
}
