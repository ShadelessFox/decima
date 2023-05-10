package com.shade.decima.ui.editor.core.dialog;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.editor.core.CoreNodeBinary;
import com.shade.decima.ui.editor.core.CoreNodeEntryGroup;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.editor.core.CoreTree;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class FindReferencesDialog extends BaseDialog {
    private final CoreNodeBinary root;
    private final RTTIPath[] refs;

    public FindReferencesDialog(@NotNull CoreNodeBinary root, @NotNull RTTIObject object) {
        super("Local references to '%s'".formatted(object.type().getFullTypeName()));
        this.root = root;
        this.refs = findReferences(root.getBinary(), object.get("ObjectUUID"));
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final CoreTree tree = new CoreTree(root);

        tree.getModel().setFilter(node -> {
            if (node instanceof CoreNodeEntryGroup group) {
                for (RTTIPath path : refs) {
                    final RTTIObject object = (RTTIObject) path.elements()[0].get();
                    if (object.type() == group.getType()) {
                        return true;
                    }
                }
            } else if (node instanceof CoreNodeObject object) {
                for (RTTIPath path : refs) {
                    if (path.startsWith(object.getPath())) {
                        return true;
                    }
                }
            }

            return false;
        });

        tree.setRootVisible(false);

        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0", "grow,fill,500lp"));
        panel.add(new JScrollPane(tree), "h min(pref, 200lp)");

        return panel;
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_OK};
    }

    @NotNull
    private static RTTIPath[] findReferences(@NotNull CoreBinary binary, @NotNull RTTIObject uuid) {
        final List<RTTIPath> paths = new ArrayList<>();
        for (RTTIObject entry : binary.entries()) {
            final List<RTTIPathElement> curPath = new ArrayList<>();
            curPath.add(new RTTIPathElement.UUID(entry));
            findReferences(entry, uuid, paths, curPath);
        }
        return paths.toArray(RTTIPath[]::new);
    }

    private static void findReferences(
        @NotNull Object root,
        @NotNull RTTIObject uuid,
        @NotNull List<RTTIPath> paths,
        @NotNull List<RTTIPathElement> curPath
    ) {
        if (root instanceof RTTIReference.Internal ref && ref.uuid().equals(uuid)) {
            paths.add(new RTTIPath(curPath.toArray(RTTIPathElement[]::new)));
        } else if (root instanceof RTTIObject object) {
            for (RTTIClass.Field<?> field : object.type().getFields()) {
                curPath.add(new RTTIPathElement.Field(field));
                findReferences(object.get(field), uuid, paths, curPath);
                curPath.remove(curPath.size() - 1);
            }
        } else if (root instanceof Object[] objects) {
            for (int i = 0; i < objects.length; i++) {
                curPath.add(new RTTIPathElement.Index(i));
                findReferences(objects[i], uuid, paths, curPath);
                curPath.remove(curPath.size() - 1);
            }
        }
    }
}
