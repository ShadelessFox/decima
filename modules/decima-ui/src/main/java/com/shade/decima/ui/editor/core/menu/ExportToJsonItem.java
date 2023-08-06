package com.shade.decima.ui.editor.core.menu;

import com.google.gson.stream.JsonWriter;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Export to JSON\u2026", icon = "Action.exportIcon", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT, order = 4000)
public class ExportToJsonItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final ValueController<Object> controller = Objects.requireNonNull(editor.getValueController());

        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as");
        chooser.setFileFilter(new FileExtensionFilter("JSON Files", "json"));
        chooser.setSelectedFile(new File("exported.json"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showSaveDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(chooser.getSelectedFile().toPath()))) {
            writer.setLenient(false);
            writer.setIndent("\t");

            serialize(controller.getValue(), controller.getValueType(), writer);
        } catch (IOException e) {
            UIUtils.showErrorDialog(e, "Can't export as JSON");
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.EDITOR_KEY) instanceof CoreEditor editor && editor.getValueController() != null;
    }

    private static void serialize(@NotNull Object object, @NotNull RTTIType<?> type, @NotNull JsonWriter writer) throws IOException {
        if (object instanceof RTTIObject obj) {
            writer.beginObject();

            writer.name("$type");
            writer.value(obj.type().getFullTypeName());

            for (RTTIClass.Field<?> field : obj.type().getFields()) {
                final Object value = field.get((RTTIObject) object);

                if (value != null) {
                    writer.name(field.getName());
                    serialize(value, field.getType(), writer);
                }
            }

            writer.endObject();
        } else if (object instanceof RTTIReference) {
            if (object instanceof RTTIReference.Internal ref) {
                writer.value("<internal " + (ref.kind() == RTTIReference.Kind.LINK ? "link" : "reference") + " to " + RTTIUtils.uuidToString(ref.uuid()) + ">");
            } else if (object instanceof RTTIReference.External ref) {
                writer.value("<external " + (ref.kind() == RTTIReference.Kind.LINK ? "link" : "reference") + " to " + ref.path() + ", " + RTTIUtils.uuidToString(ref.uuid()) + ">");
            } else {
                writer.value("<empty reference>");
            }
        } else if (type instanceof RTTITypeArray<?> array) {
            writer.beginArray();

            for (int i = 0, length = array.length(object); i < length; i++) {
                serialize(array.get(object, i), array.getComponentType(), writer);
            }

            writer.endArray();
        } else if (object instanceof String value) {
            writer.value(value);
        } else if (object instanceof Number value) {
            writer.value(value);
        } else if (object instanceof Boolean value) {
            writer.value(value);
        } else if (object instanceof RTTITypeEnum.Constant constant) {
            writer.value(constant.name());
        } else {
            writer.value("<unsupported type '" + type.getFullTypeName() + "'>");
        }
    }
}
