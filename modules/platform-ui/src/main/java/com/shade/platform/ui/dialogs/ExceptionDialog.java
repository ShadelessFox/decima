package com.shade.platform.ui.dialogs;

import com.shade.platform.ui.controls.LabeledSeparator;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDialog extends BaseDialog {
    private final Throwable throwable;

    public ExceptionDialog(@NotNull Throwable throwable, @NotNull String title) {
        super(title);
        this.throwable = throwable;
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0,wrap", "[grow,fill]", "[][fill][][grow,fill]"));

        {
            final JTextArea view = new JTextArea(throwable.getMessage());
            view.setFont(new Font(Font.MONOSPACED, view.getFont().getStyle(), view.getFont().getSize()));
            view.setEditable(false);
            view.setLineWrap(true);

            panel.add(new LabeledSeparator("Message"));
            panel.add(new JScrollPane(view), "h min(50lp, pref)::");
        }

        {
            final JTextArea view = new JTextArea(getStackTrace());
            view.setFont(new Font(Font.MONOSPACED, view.getFont().getStyle(), view.getFont().getSize()));
            view.setEditable(false);

            panel.add(new LabeledSeparator("Stack trace"));
            panel.add(new JScrollPane(view));
        }

        panel.setPreferredSize(new Dimension(700, 400));

        return panel;
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        if (descriptor == BUTTON_COPY) {
            final StringSelection contents = new StringSelection(getStackTrace());
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            clipboard.setContents(contents, contents);
            return;
        }

        super.buttonPressed(descriptor);
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_COPY, BUTTON_OK};
    }

    @NotNull
    private String getStackTrace() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        throwable.printStackTrace(pw);

        return sw.toString().replace("\t", "    ");
    }
}
