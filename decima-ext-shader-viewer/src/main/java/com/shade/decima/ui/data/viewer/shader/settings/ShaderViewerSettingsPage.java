package com.shade.decima.ui.data.viewer.shader.settings;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.controls.validators.ExistingFileValidator;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.Objects;

@SettingsPageRegistration(parent = "coreEditor", id = "shader", name = "Shader Viewer")
public class ShaderViewerSettingsPage implements SettingsPage {
    private JTextField d3dCompilerPath;
    private JTextField dxCompilerPath;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        {
            final FileExtensionFilter filter = FileExtensionFilter.ofNativeLibrary("Direct3D compiler library");

            d3dCompilerPath = new JTextField();
            d3dCompilerPath.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "d3dcompiler.dll");
            UIUtils.addOpenFileAction(d3dCompilerPath, "Select Direct3D compiler library", filter);
            UIUtils.installInputValidator(d3dCompilerPath, new ExistingFileValidator(d3dCompilerPath, filter, false), listener);
        }

        {
            final FileExtensionFilter filter = FileExtensionFilter.ofNativeLibrary("DirectX compiler library");

            dxCompilerPath = new JTextField();
            dxCompilerPath.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dxcompiler.dll");
            UIUtils.addOpenFileAction(dxCompilerPath, "Select DirectX compiler library", filter);
            UIUtils.installInputValidator(dxCompilerPath, new ExistingFileValidator(dxCompilerPath, filter, false), listener);
        }

        final JPanel tools = new JPanel();
        tools.setBorder(new LabeledBorder("Disassemble"));
        tools.setLayout(new MigLayout("ins panel,wrap", "[fill][grow,fill,400lp]", ""));

        tools.add(new JLabel("Direct3D compiler library:"));
        tools.add(d3dCompilerPath);

        tools.add(new JLabel("DirectX compiler library:"));
        tools.add(dxCompilerPath);

        tools.add(UIUtils.createInfoLabel("These files are shipped with the game itself and located within the game directory"), "span");

        return tools;
    }

    @Override
    public void apply() {
        final ShaderViewerSettings settings = ShaderViewerSettings.getInstance();
        settings.d3dCompilerPath = IOUtils.getTrimmedOrNullIfEmpty(d3dCompilerPath.getText());
        settings.dxCompilerPath = IOUtils.getTrimmedOrNullIfEmpty(dxCompilerPath.getText());
    }

    @Override
    public void reset() {
        final ShaderViewerSettings settings = ShaderViewerSettings.getInstance();
        d3dCompilerPath.setText(settings.d3dCompilerPath);
        dxCompilerPath.setText(settings.dxCompilerPath);
    }

    @Override
    public boolean isModified() {
        final ShaderViewerSettings settings = ShaderViewerSettings.getInstance();
        return !Objects.equals(IOUtils.getTrimmedOrNullIfEmpty(d3dCompilerPath.getText()), settings.d3dCompilerPath)
            || !Objects.equals(IOUtils.getTrimmedOrNullIfEmpty(dxCompilerPath.getText()), settings.dxCompilerPath);
    }

    @Override
    public boolean isComplete() {
        return UIUtils.isValid(d3dCompilerPath)
            && UIUtils.isValid(dxCompilerPath);
    }
}
