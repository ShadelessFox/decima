package com.shade.decima.ui.data.viewer;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.editor.Editor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class LocalizedTextResourceViewer implements ValueViewer {
    public static final LocalizedTextResourceViewer INSTANCE = new LocalizedTextResourceViewer();

    @NotNull
    @Override
    public JComponent createComponent() {
        return new JTable(new DefaultTableModel(new Object[]{"Language", "Text", "Notes", "Flags"}, 25));
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final RTTIObject value = (RTTIObject) editor.getController().getSelectedValue();

        if (value != null) {
            final RTTITypeEnum language = (RTTITypeEnum) editor.getInput().getProject().getTypeRegistry().find("ELanguage");
            final RTTIObject[] entries = value.get("Entries");
            final TableModel model = ((JTable) component).getModel();

            for (int i = 0; i < entries.length; i++) {
                final RTTIObject entry = entries[i];

                model.setValueAt(language.valueOf(i + 1).name(), i, 0);
                model.setValueAt(entry.get("Text"), i, 1);
                model.setValueAt(entry.get("Notes"), i, 2);
                model.setValueAt(entry.get("Flags"), i, 3);
            }
        }
    }
}
