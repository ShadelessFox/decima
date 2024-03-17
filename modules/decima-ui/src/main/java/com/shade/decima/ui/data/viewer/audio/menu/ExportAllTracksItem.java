package com.shade.decima.ui.data.viewer.audio.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.data.viewer.audio.AudioPlayerPanel;
import com.shade.decima.ui.data.viewer.audio.AudioPlayerUtils;
import com.shade.decima.ui.data.viewer.audio.Playlist;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.IntStream;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_AUDIO_PLAYER_ID, name = "Export &All Tracks\u2026", icon = "Action.exportIcon", group = CTX_MENU_AUDIO_PLAYER_GROUP_GENERAL, order = 1000)
public class ExportAllTracksItem extends MenuItem {
    private static final Logger log = LoggerFactory.getLogger(ExportAllTracksItem.class);

    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save tracks to");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.addChoosableFileFilter(new FileExtensionFilter("OGG Files", "ogg"));
        chooser.addChoosableFileFilter(new FileExtensionFilter("WAV Files", "wav"));
        chooser.addChoosableFileFilter(new FileExtensionFilter("MP3 Files", "mp3"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final Path output = chooser.getSelectedFile().toPath();
        final String extension = ((FileExtensionFilter) chooser.getFileFilter()).getExtension();

        final Playlist playlist = ctx.getData(AudioPlayerPanel.PLAYLIST_KEY);
        final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);
        final int[] indices = getIndices(ctx);

        ProgressDialog.showProgressDialog(null, "Exporting tracks", monitor -> {
            try (var task = monitor.begin("Export tracks", indices.length)) {
                for (int index : indices) {
                    if (task.isCanceled()) {
                        break;
                    }

                    try {
                        AudioPlayerUtils.extractTrack(task.split(1), project, playlist, index, output.resolve(playlist.getName(index) + '.' + extension));
                    } catch (IOException | InterruptedException e) {
                        log.error("Error extracting track #{} ({})", index, playlist.getName(index), e);
                    }
                }
            }

            return null;
        });
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return getIndices(ctx).length > 0;
    }

    @NotNull
    protected int[] getIndices(@NotNull MenuItemContext ctx) {
        return IntStream
            .range(0, ctx.getData(AudioPlayerPanel.PLAYLIST_KEY).size())
            .toArray();
    }
}
