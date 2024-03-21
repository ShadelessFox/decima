package com.shade.decima.ui.data.viewer.model.menu;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.BaseEditDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, name = "Teleport\u2026", description = "Teleport to position", icon = "Action.navigateIcon", keystroke = "ctrl G", group = BAR_MODEL_VIEWER_GROUP_MISC, order = 1000)
public class TeleportToItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ModelViewport viewport = ctx.getData(ModelViewport.VIEWPORT_KEY);
        final Camera camera = viewport.getCamera();
        final TeleportToDialog dialog = new TeleportToDialog(camera.getPosition());

        if (dialog.showDialog(null) == BaseDialog.BUTTON_OK) {
            camera.setPosition(dialog.getPosition());
        }
    }

    private static class TeleportToDialog extends BaseEditDialog {
        private final JSpinner xSpinner;
        private final JSpinner ySpinner;
        private final JSpinner zSpinner;

        public TeleportToDialog(@NotNull Vector3fc v) {
            super("Teleport To");

            this.xSpinner = new JSpinner(new SpinnerNumberModel(v.x(), null, null, 0.1f));
            this.ySpinner = new JSpinner(new SpinnerNumberModel(v.y(), null, null, 0.1f));
            this.zSpinner = new JSpinner(new SpinnerNumberModel(v.z(), null, null, 0.1f));
        }

        @NotNull
        @Override
        protected JComponent createContentsPane() {
            final JPanel panel = new JPanel();
            panel.setLayout(new MigLayout("ins 0,wrap", "[][grow,fill]"));

            final JLabel xLabel = Mnemonic.resolve(new JLabel("&X:"));
            final JLabel yLabel = Mnemonic.resolve(new JLabel("&Y:"));
            final JLabel zLabel = Mnemonic.resolve(new JLabel("&Z:"));

            panel.add(xLabel);
            panel.add(xSpinner);

            panel.add(yLabel);
            panel.add(ySpinner);

            panel.add(zLabel);
            panel.add(zSpinner);

            xLabel.setLabelFor(xSpinner);
            yLabel.setLabelFor(ySpinner);
            zLabel.setLabelFor(zSpinner);

            return panel;
        }

        @Override
        protected boolean isComplete() {
            return UIUtils.isValid(xSpinner)
                && UIUtils.isValid(ySpinner)
                && UIUtils.isValid(zSpinner);
        }

        @NotNull
        public Vector3f getPosition() {
            return new Vector3f(
                (float) xSpinner.getValue(),
                (float) ySpinner.getValue(),
                (float) zSpinner.getValue()
            );
        }
    }
}
