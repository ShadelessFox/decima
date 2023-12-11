package com.shade.decima.ui.data.viewer.audio;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.viewer.audio.controls.AudioPlayerComponent;
import com.shade.decima.ui.data.viewer.audio.playlists.*;
import com.shade.decima.ui.data.viewer.audio.settings.AudioPlayerSettings;
import com.shade.decima.ui.data.viewer.audio.wwise.WwiseMedia;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

public class AudioPlayerPanel extends JPanel implements Disposable {
    public static final DataKey<Playlist> PLAYLIST_KEY = new DataKey<>("playlist", Playlist.class);
    public static final DataKey<int[]> SELECTION_KEY = new DataKey<>("selection", int[].class);

    private final PlaylistList list = new PlaylistList();
    private final AudioPlayerComponent player;

    private Project project;
    private Playlist playlist;

    public AudioPlayerPanel() {
        player = new AudioPlayerComponent() {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.shadow")));
            }

            @Override
            protected boolean previousTrackRequested() {
                final int index = list.getSelectedIndex();

                if (index >= 0) {
                    final int wrapped = IOUtils.wrapAround(index - 1, list.getModel().getSize());
                    list.setSelectedIndex(wrapped);
                    list.scrollRectToVisible(list.getCellBounds(wrapped, wrapped));
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            protected boolean nextTrackRequested() {
                final int index = list.getSelectedIndex();

                if (index >= 0) {
                    final int wrapped = IOUtils.wrapAround(index + 1, list.getModel().getSize());
                    list.setSelectedIndex(wrapped);
                    list.scrollRectToVisible(list.getCellBounds(wrapped, wrapped));
                    return true;
                } else {
                    return false;
                }
            }
        };

        setLayout(new MigLayout("ins 0,gap 0", "[grow,fill]", "[grow,fill][]"));
        add(UIUtils.createBorderlessScrollPane(list), "wrap");
        add(player);
        setPreferredSize(new Dimension(250, 0));

        MenuManager.getInstance().installContextMenu(list, MenuConstants.CTX_MENU_AUDIO_PLAYER_ID, key -> switch (key) {
            case "playlist" -> playlist;
            case "selection" -> list.getSelectedIndices();
            case "project" -> project;
            default -> null;
        });
    }

    public void setInput(@NotNull Project project, @NotNull RTTIObject object) {
        this.project = project;
        this.playlist = switch (object.type().getTypeName()) {
            case "WwiseBankResource" -> new WwiseBankPlaylist(object);
            case "WwiseWemResource" -> new WwiseWemPlaylist(object);
            case "WwiseWemLocalizedResource" -> new WwiseWemLocalizedPlaylist(object);
            case "LocalizedSimpleSoundResource" -> new LocalizedSoundPlaylist(object);
            case "WaveResource" -> new WavePlaylist(object);
            default -> throw new IllegalArgumentException("Unsupported type: " + object.type().getTypeName());
        };

        this.list.setPlaylist(playlist);
        this.player.setClip(null);
    }

    @Override
    public void dispose() {
        list.setPlaylist(null);
        player.close();
    }

    private void setTrack(int index) {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();
        final String codebooks = settings.ww2oggCodebooksPath;
        final String ww2ogg = settings.ww2oggPath;
        final String revorb = settings.revorbPath;
        final String ffmpeg = settings.ffmpegPath;

        if (codebooks == null || ww2ogg == null || revorb == null || ffmpeg == null) {
            JOptionPane.showMessageDialog(
                JOptionPane.getRootFrame(),
                "<html>One or more native tools required for audio playback are missing.<br><br>You can specify them in <kbd>File</kbd> &rArr; <kbd>Settings</kbd> &rArr; <kbd>Audio Player</kbd></html>",
                "Can't play audio",
                JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        ProgressDialog.showProgressDialog(JOptionPane.getRootFrame(), "Prepare to play audio", monitor -> {
            try {
                final Path path = Files.createTempFile(null, ".wav");

                try {
                    AudioPlayerUtils.extractTrack(monitor, project, playlist, index, path);
                    player.setClip(AudioPlayerUtils.openClip(path.toFile()));
                } finally {
                    Files.deleteIfExists(path);
                }
            } catch (Exception e) {
                UIUtils.showErrorDialog(e, "Error playing audio");
            }

            return null;
        });
    }

    private class PlaylistList extends JList<Track> {
        public PlaylistList() {
            setCellRenderer(new ColoredListCellRenderer<>() {
                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends Track> list, @NotNull Track track, int index, boolean selected, boolean focused) {
                    append("[%d] ".formatted(index), TextAttributes.GRAYED_ATTRIBUTES);
                    append(track.name, TextAttributes.REGULAR_ATTRIBUTES);

                    if (track.duration != null) {
                        append(" " + UIUtils.formatDuration(track.duration), TextAttributes.GRAYED_ATTRIBUTES);
                    }
                }
            });
            addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && getSelectedIndex() >= 0) {
                    setTrack(getSelectedIndex());
                }
            });
        }

        public void setPlaylist(@Nullable Playlist playlist) {
            if (getModel() instanceof PlaylistListModel model) {
                model.dispose();
            }

            if (playlist != null) {
                setModel(new PlaylistListModel(playlist));
            } else {
                setModel(new DefaultListModel<>());
            }
        }
    }

    private class PlaylistListModel extends AbstractListModel<Track> {
        private final Track[] tracks;
        private final SwingWorker<Void, Integer> worker;

        public PlaylistListModel(@NotNull Playlist playlist) {
            this.tracks = IntStream.range(0, playlist.size())
                .mapToObj(i -> new Track(playlist.getName(i), null))
                .toArray(Track[]::new);

            this.worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    for (int i = 0; i < tracks.length; i++) {
                        if (isCancelled()) {
                            break;
                        }

                        try {
                            final ByteBuffer buffer = ByteBuffer.wrap(playlist.getData(project.getPackfileManager(), i)).order(ByteOrder.LITTLE_ENDIAN);
                            final WwiseMedia media = WwiseMedia.read(buffer);
                            final Duration duration = media.get(WwiseMedia.Chunk.Type.FMT).getDuration();

                            tracks[i] = new Track(tracks[i].name, duration);
                            publish(i);
                        } catch (Exception ignored) {
                        }
                    }

                    return null;
                }

                @Override
                protected void process(List<Integer> indices) {
                    for (Integer index : indices) {
                        fireContentsChanged(this, index, index);
                    }
                }
            };

            worker.execute();
        }

        public void dispose() {
            worker.cancel(false);
        }

        @Override
        public int getSize() {
            return tracks.length;
        }

        @Override
        public Track getElementAt(int index) {
            return tracks[index];
        }
    }

    private record Track(@NotNull String name, @Nullable Duration duration) {}
}
