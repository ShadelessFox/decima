package com.shade.decima.ui.data.viewer.shader;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwShader;
import com.shade.decima.model.util.CloseableLibrary;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.viewer.shader.com.*;
import com.shade.decima.ui.data.viewer.shader.settings.ShaderViewerSettings;
import com.shade.platform.ui.controls.FileChooser;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.sun.jna.ptr.PointerByReference;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Objects;

public class ShaderViewerPanel extends JComponent {
    private final JTabbedPane pane;
    private HwShader shader;

    public ShaderViewerPanel() {
        this.pane = new JTabbedPane();

        setLayout(new BorderLayout());
        add(pane, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 0);
    }

    public void setInput(@NotNull ValueController<RTTIObject> controller) {
        final ByteBuffer buffer = ByteBuffer
            .wrap(controller.getValue().get("ExtraData"))
            .order(ByteOrder.LITTLE_ENDIAN);

        this.shader = HwShader.read(buffer, controller.getProject().getContainer().getType());
        updateTabs();
    }

    private void updateTabs() {
        pane.removeAll();

        for (HwShader.Entry entry : shader.programs()) {
            if (entry.program().blob().length == 0) {
                continue;
            }

            pane.addTab(entry.programType().name(), new ProgramPanel(entry));
        }
    }

    private static class ProgramPanel extends JComponent {
        public ProgramPanel(@NotNull HwShader.Entry entry) {
            final JTextArea area = new JTextArea("// No decompiled data");
            area.setFont(new Font(Font.MONOSPACED, area.getFont().getStyle(), area.getFont().getSize()));
            area.setEditable(false);

            final JButton button = new JButton("Decompile");
            button.setMnemonic('D');
            button.addActionListener(e -> {
                final String text = decompile(entry);
                area.setText(text);
            });

            final JToolBar mainToolbar = new JToolBar();
            mainToolbar.add(new ExportAction(entry));
            mainToolbar.add(button);

            setLayout(new MigLayout("ins panel,wrap", "[grow,fill]", "[grow,fill][]"));
            add(new JScrollPane(area));
            add(mainToolbar);
        }

        private class ExportAction extends AbstractAction {
            final private byte[] programBlob;

            public ExportAction(@NotNull HwShader.Entry entry) {
                programBlob = entry.program().blob();
                putValue(SMALL_ICON, UIManager.getIcon("Action.exportIcon"));
                putValue(SHORT_DESCRIPTION, "Export binary data");
                setEnabled(true);
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                final JFileChooser chooser = new FileChooser();
                chooser.setDialogTitle("Export binary data as");
                chooser.setSelectedFile(new File("exported.bin"));
                chooser.setAcceptAllFileFilterUsed(true);

                if (chooser.showSaveDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                try {
                    Files.write(chooser.getSelectedFile().toPath(), programBlob);
                } catch (IOException e) {
                    UIUtils.showErrorDialog(e, "Error exporting data");
                }
            }
        }

        @NotNull
        private static String decompile(@NotNull HwShader.Entry entry) {
            if (entry.shaderModel() > 5) {
                return decompileDXIL(entry.program().blob());
            } else {
                return decompileDXBC(entry.program().blob());
            }
        }

        @NotNull
        private static String decompileDXIL(@NotNull byte[] data) {
            try (DXCompiler library = CloseableLibrary.load(
                Objects.requireNonNullElse(ShaderViewerSettings.getInstance().dxCompilerPath, "dxcompiler.dll"),
                DXCompiler.class
            )) {
                final PointerByReference dxcUtilsPtr = new PointerByReference();
                checkRc(library.DxcCreateInstance(IDxcUtils.CLSID_DxcUtils, IDxcUtils.IID_IDxcUtils, dxcUtilsPtr));
                final IDxcUtils dxcUtils = new IDxcUtils(dxcUtilsPtr.getValue());

                final PointerByReference dxcCompilerPtr = new PointerByReference();
                checkRc(library.DxcCreateInstance(IDxcCompiler.CLSID_DxcCompiler, IDxcCompiler.IID_IDxcCompiler, dxcCompilerPtr));
                final IDxcCompiler dxcCompiler = new IDxcCompiler(dxcCompilerPtr.getValue());

                final IDxcBlobEncoding source = new IDxcBlobEncoding();
                dxcUtils.CreateBlob(data, 0, source);

                final IDxcBlobEncoding disassembly = new IDxcBlobEncoding();
                dxcCompiler.Disassemble(source, disassembly);

                try {
                    return disassembly.getString();
                } finally {
                    source.Release();
                    disassembly.Release();
                }
            } catch (UnsatisfiedLinkError e) {
                throw new IllegalStateException("Can't find DirectX compiler library. You can specify path to the compiler in File | Settings | Core Editor | Shader Viewer.", e);
            }
        }

        @NotNull
        private static String decompileDXBC(@NotNull byte[] data) {
            try (D3DCompiler library = CloseableLibrary.load(
                Objects.requireNonNullElse(ShaderViewerSettings.getInstance().d3dCompilerPath, "d3dcompiler_47.dll"),
                D3DCompiler.class
            )) {
                final PointerByReference disassemblyPtr = new PointerByReference();
                checkRc(library.D3DDisassemble(data, data.length, 0, null, disassemblyPtr));

                final IDxcBlob disassembly = new IDxcBlob(disassemblyPtr.getValue());

                try {
                    return disassembly.getString();
                } finally {
                    disassembly.Release();
                }
            } catch (UnsatisfiedLinkError e) {
                throw new IllegalStateException("Can't find Direct3D compiler library. You can specify path to the compiler in File | Settings | Core Editor | Shader Viewer.", e);
            }
        }

        private static void checkRc(int rc) {
            if (rc < 0) {
                throw new IllegalStateException("Error: %#10x".formatted(rc));
            }
        }
    }

    private interface DXCompiler extends CloseableLibrary {
        int DxcCreateInstance(GUID rclsid, GUID riid, PointerByReference ppv);
    }

    private interface D3DCompiler extends CloseableLibrary {
        int D3DDisassemble(byte[] srcBuf, int srcLen, int flags, String comments, PointerByReference disassembly);
    }
}
