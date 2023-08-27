package com.shade.decima.ui.data.viewer.shader;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwShader;
import com.shade.decima.ui.data.ValueController;
import com.shade.util.NotNull;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

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

        for (HwShader.Program.Entry entry : shader.programs()) {
            if (entry.program().dxbc().length == 0) {
                continue;
            }

            pane.addTab(entry.programType().name(), new ProgramPanel(entry));
        }
    }

    private static class ProgramPanel extends JComponent {
        public ProgramPanel(@NotNull HwShader.Program.Entry entry) {
            final JTextArea area = new JTextArea("// No decompiled data");
            area.setFont(new Font(Font.MONOSPACED, area.getFont().getStyle(), area.getFont().getSize()));
            area.setEditable(false);

            final JButton button = new JButton("Decompile");
            button.setMnemonic('D');
            button.addActionListener(e -> {
                final byte[] dxbc = entry.program().dxbc();
                final String text = decompile(dxbc);
                area.setText(text);
            });

            setLayout(new MigLayout("ins panel,wrap", "[grow,fill]", "[grow,fill][]"));
            add(new JScrollPane(area));
            add(button);
        }

        @NotNull
        private static String decompile(@NotNull byte[] dxbc) {
            final var result = new PointerByReference();
            final var code = D3DCompiler.INSTANCE.D3DDisassemble(dxbc, dxbc.length, 0, null, result);

            if (code < 0) {
                return "// Disassembly failed: %#x".formatted(code & 0xFFFFFFFFL);
            }

            // interface ID3D10Blob {
            //     ID3D10BlobVtbl *lpVtbl
            // }
            final var blob = result.getPointer().getPointer(0);

            // interface ID3D10BlobVtbl {
            //     HRESULT (*QueryInterface)   (ID3D10Blob * This, REFIID riid, void **ppvObject);
            //     ULONG   (*AddRef)           (ID3D10Blob * This);
            //     ULONG   (*Release)          (ID3D10Blob * This);
            //     LPVOID  (*GetBufferPointer) (ID3D10Blob * This);
            //     SIZE_T  (*GetBufferSize)    (ID3D10Blob * This);
            // }
            final var vtbl = blob.getPointer(0).getPointerArray(0, 5);

            final var GetBufferPointer = Function.getFunction(vtbl[3]);
            final var GetBufferSize = Function.getFunction(vtbl[4]);
            final var Release = Function.getFunction(vtbl[2]);

            final var size = (int) GetBufferSize.invoke(int.class, new Object[]{blob});
            final var data = (Pointer) GetBufferPointer.invoke(Pointer.class, new Object[]{blob});
            final var text = new String(data.getByteArray(0, size), StandardCharsets.UTF_8);

            Release.invoke(int.class, new Object[]{blob});

            return text;
        }
    }

    private interface D3DCompiler extends Library {
        D3DCompiler INSTANCE = Native.load("D3DCompiler_47", D3DCompiler.class);

        int D3DDisassemble(byte[] srcBuf, int srcLen, int flags, String comments, PointerByReference disassembly);
    }
}
