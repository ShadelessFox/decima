package com.shade.decima.ui.data.viewer;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

@ValueViewerRegistration(@Type(type = RTTIReference.class))
public class ReferenceValueViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new JPanel(new BorderLayout());
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final ObjectWithViewer data = Objects.requireNonNull(getObjectWithViewer(controller));
        final JComponent delegate = data.viewer.createComponent();

        component.removeAll();
        component.add(delegate, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> data.viewer.refresh(delegate, new ObjectValueController(controller, data.object)));
    }

    @Override
    public boolean canView(@NotNull ValueController<?> controller) {
        return getObjectWithViewer(controller) != null;
    }

    @Nullable
    private static ObjectWithViewer getObjectWithViewer(@NotNull ValueController<?> controller) {
        final RTTIReference reference = (RTTIReference) controller.getValue();
        final RTTIObject object;

        try {
            object = reference.get(controller.getProject(), ((CoreEditor) controller.getEditor()).getBinary());
        } catch (IOException e) {
            return null;
        }

        if (object == null) {
            return null;
        }

        final ValueViewer viewer = ValueRegistry.getInstance().findViewer(
            object,
            object.type(),
            controller.getProject().getContainer().getType()
        );

        if (viewer == null) {
            return null;
        }

        return new ObjectWithViewer(object, viewer);
    }

    private static record ObjectWithViewer(@NotNull RTTIObject object, @NotNull ValueViewer viewer) {}

    private static record ObjectValueController(@NotNull ValueController<?> delegate, @NotNull RTTIObject object) implements ValueController<RTTIObject> {
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
        public RTTIObject getValue() {
            return object;
        }

        @Override
        public void setValue(@NotNull RTTIObject value) {
            // not implemented
        }
    }
}
