package com.shade.decima.ui.editor.core.dialog;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypePrimitive;
import com.shade.decima.model.rtti.types.RTTITypeReference;
import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.decima.ui.controls.hex.impl.DefaultHexModel;
import com.shade.decima.ui.data.ValueController;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class InspectValueDialog<T> extends BaseDialog {
    private final Project project;
    private final ValueController<T> controller;

    public InspectValueDialog(@NotNull Project project, @NotNull ValueController<T> controller) {
        super("Inspect '%s'".formatted(controller.getValueLabel()));
        this.project = project;
        this.controller = controller;
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final RTTIType<T> type = controller.getValueType();

        final byte[] data = new byte[type.getSize(project.getTypeRegistry(), controller.getValue())];
        type.write(project.getTypeRegistry(), ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), controller.getValue());

        final HexEditor editor = new HexEditor();
        editor.setModel(new DefaultHexModel(data));

        final JScrollPane editorPane = new JScrollPane(editor);
        editorPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0", "[fill][grow,fill]", "[top][top][top,grow]"));

        panel.add(new JLabel("Kind: "));
        panel.add(new JLabel(getTypeKind(type)), "wrap");

        panel.add(new JLabel("Type: "));
        panel.add(new JLabel(getTypeHierarchy(type)), "wrap");

        panel.add(new JLabel("Size: "));
        panel.add(new JLabel("%d bytes".formatted(data.length)), "wrap");

        panel.add(new JLabel("Data: "));
        panel.add(editorPane, "h min(pref, 200lp)");

        return panel;
    }

    @NotNull
    @Override
    protected JDialog createDialog(@Nullable Window owner) {
        final JDialog dialog = super.createDialog(owner);
        dialog.setResizable(false);
        return dialog;
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_OK};
    }

    @NotNull
    private static String getTypeKind(@NotNull RTTIType<?> type) {
        if (type instanceof RTTITypeEnum e) {
            return e.isEnumSet() ? "Enum Flags" : "Enum";
        } else if (type instanceof RTTITypePrimitive) {
            return "Primitive";
        } else if (type instanceof RTTITypeReference) {
            return "Reference";
        } else if (type instanceof RTTITypeContainer) {
            return "Container";
        } else if (type instanceof RTTIClass) {
            return "Class";
        } else {
            return "Unknown";
        }
    }

    @NotNull
    private static String getTypeHierarchy(@NotNull RTTIType<?> type) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        getTypeHierarchy(type, sb, 0);
        sb.append("</html>");
        return sb.toString();
    }

    private static void getTypeHierarchy(@NotNull RTTIType<?> type, @NotNull StringBuilder sb, int depth) {
        if (depth > 0) {
            sb.append("<br>&nbsp;");
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;".repeat(depth - 1));
            sb.append("\u2570 ");
        }

        appendTypeName(type, sb);

        if (type instanceof RTTIClass cls) {
            for (RTTIClass.Superclass superclass : cls.getSuperclasses()) {
                getTypeHierarchy(superclass.getType(), sb, depth + 1);
            }
        }
    }

    private static void appendTypeName(@NotNull RTTIType<?> type, @NotNull StringBuilder sb) {
        sb.append(type.getTypeName());

        if (type instanceof RTTITypeParameterized<?, ?> p) {
            sb.append("&lt;");
            appendTypeName(p.getComponentType(), sb);
            sb.append("&gt;");
        }
    }
}
