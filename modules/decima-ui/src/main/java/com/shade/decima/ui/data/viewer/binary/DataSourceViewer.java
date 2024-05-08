package com.shade.decima.ui.data.viewer.binary;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.io.UncheckedIOException;

@ValueViewerRegistration({
    @Selector(type = @Type(type = HwDataSource.class))
})
public class DataSourceViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new BinaryViewerPanel();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void refresh(@NotNull ProgressMonitor monitor, @NotNull JComponent component, @NotNull ValueController<?> controller) {
        final var panel = (BinaryViewerPanel) component;
        panel.setController(new ProxyValueController((ValueController<RTTIObject>) controller));
    }

    private record ProxyValueController(@NotNull ValueController<RTTIObject> controller) implements ValueController<byte[]> {
        @NotNull
        @Override
        public RTTIType<byte[]> getValueType() {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public RTTIPath getValuePath() {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public String getValueLabel() {
            return controller.getValueLabel();
        }

        @NotNull
        @Override
        public Editor getEditor() {
            return controller.getEditor();
        }

        @NotNull
        @Override
        public Project getProject() {
            return controller.getProject();
        }

        @NotNull
        @Override
        public RTTICoreFile getCoreFile() {
            return controller.getCoreFile();
        }

        @NotNull
        @Override
        public byte[] getValue() {
            final Project project = controller.getProject();
            final HwDataSource dataSource = controller.getValue().cast();

            try {
                return dataSource.getData(project.getPackfileManager());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
