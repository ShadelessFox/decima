package com.shade.decima.ui.data.viewer.model.menu;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.controls.validation.Validation;
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
import javax.swing.text.JTextComponent;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, icon = "Action.navigateIcon", description = "Teleport to position", keystroke = "ctrl G", group = BAR_MODEL_VIEWER_GROUP_MISC, order = 1000)
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
        private final JTextField xField;
        private final JTextField yField;
        private final JTextField zField;

        public TeleportToDialog(@NotNull Vector3fc v) {
            super("Teleport To");

            this.xField = new JTextField(String.valueOf(v.x()));
            this.yField = new JTextField(String.valueOf(v.y()));
            this.zField = new JTextField(String.valueOf(v.z()));
        }

        @NotNull
        @Override
        protected JComponent createContentsPane() {
            final JPanel panel = new JPanel();
            panel.setLayout(new MigLayout("ins 0", "[][grow,fill][][grow,fill][][grow,fill]"));

            UIUtils.installInputValidator(xField, new DoubleValidator(xField), this);
            UIUtils.installInputValidator(yField, new DoubleValidator(yField), this);
            UIUtils.installInputValidator(zField, new DoubleValidator(zField), this);

            final JLabel xLabel = Mnemonic.resolve(new JLabel("&X:"));
            final JLabel yLabel = Mnemonic.resolve(new JLabel("&Y:"));
            final JLabel zLabel = Mnemonic.resolve(new JLabel("&Z:"));

            panel.add(xLabel);
            panel.add(xField);

            panel.add(yLabel);
            panel.add(yField);

            panel.add(zLabel);
            panel.add(zField);

            xLabel.setLabelFor(xField);
            yLabel.setLabelFor(yField);
            zLabel.setLabelFor(zField);

            return panel;
        }

        @Override
        protected boolean isComplete() {
            return UIUtils.isValid(xField)
                && UIUtils.isValid(yField)
                && UIUtils.isValid(zField);
        }

        @NotNull
        public Vector3f getPosition() {
            return new Vector3f(
                Float.parseFloat(xField.getText().trim()),
                Float.parseFloat(yField.getText().trim()),
                Float.parseFloat(zField.getText().trim())
            );
        }
    }

    private static class DoubleValidator extends InputValidator {
        public DoubleValidator(@NotNull JComponent component) {
            super(component);
        }

        @NotNull
        @Override
        protected Validation validate(@NotNull JComponent input) {
            final String text = ((JTextComponent) input).getText().trim();

            try {
                Float.parseFloat(text);
                return Validation.ok();
            } catch (NumberFormatException e) {
                return Validation.error("Not a number");
            }
        }
    }
}
