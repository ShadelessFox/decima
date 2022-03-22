package com.shade.decima.ui.data.viewers;

import com.shade.decima.model.rtti.objects.RTTICollection;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.editors.EditorController;

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
    public void refresh(@NotNull JComponent component, @NotNull EditorController controller) {
        final RTTIObject value = (RTTIObject) controller.getSelectedValue();

        if (value != null) {
            final RTTITypeEnum language = (RTTITypeEnum) controller.getProject().getTypeRegistry().find("ELanguage");
            final RTTICollection<RTTIObject> entries = value.get("Entries");
            final TableModel model = ((JTable) component).getModel();

            for (int i = 0; i < entries.size(); i++) {
                final RTTIObject entry = entries.get(i);

                model.setValueAt(language.valueOf(i + 1).name(), i, 0);
                model.setValueAt(entry.get("Text"), i, 1);
                model.setValueAt(entry.get("Notes"), i, 2);
                model.setValueAt(entry.get("Flags"), i, 3);
            }
        }
    }
}
