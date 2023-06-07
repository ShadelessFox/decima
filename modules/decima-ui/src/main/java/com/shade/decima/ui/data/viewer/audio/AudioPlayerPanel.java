package com.shade.decima.ui.data.viewer.audio;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.viewer.audio.controls.AudioPlayerComponent;
import com.shade.decima.ui.data.viewer.audio.playlists.*;
import com.shade.decima.ui.data.viewer.audio.settings.AudioPlayerSettings;
import com.shade.decima.ui.data.viewer.audio.wwise.WwiseMedia;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

public class AudioPlayerPanel extends JPanel implements Disposable {
    private final PlaylistList list = new PlaylistList();
    private final AudioPlayerComponent player;

    private Project project;
    private Playlist playlist;

    public AudioPlayerPanel() {
        player = new AudioPlayerComponent() {
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
        player.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.shadow")));

        final JScrollPane playlistPane = new JScrollPane(list);
        playlistPane.setBorder(null);

        setLayout(new MigLayout("ins 0,gap 0", "[grow,fill]", "[grow,fill][]"));
        add(playlistPane, "wrap");
        add(player);
        setPreferredSize(new Dimension(250, 0));
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
            try (ProgressMonitor.Task task = monitor.begin("Prepare to play audio", 2)) {
                final byte[] data;

                try (ProgressMonitor.Task ignored = task.split(1).begin("Extract track data", 1)) {
                    data = playlist.getData(project.getPackfileManager(), index);
                }

                final Clip clip;

                if (playlist instanceof WavePlaylist wave) {
                    clip = readWaveClip(task.split(1), wave.getCodec(), data);
                } else {
                    clip = readVorbisClip(task.split(1), data);
                }

                player.setClip(clip);
            } catch (Exception e) {
                UIUtils.showErrorDialog(e, "Error playing audio");
            }

            return null;
        });
    }

    @NotNull
    private static Clip readVorbisClip(@NotNull ProgressMonitor monitor, @NotNull byte[] data) throws Exception {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();
        final Path wemPath = Files.createTempFile(null, ".wem");
        final Path oggPath = Path.of(IOUtils.getBasename(wemPath.toString()) + ".ogg");
        final Path wavPath = Path.of(IOUtils.getBasename(wemPath.toString()) + ".wav");

        try (ProgressMonitor.Task task = monitor.begin("Read vorbis clip", 3)) {
            Files.write(wemPath, data);

            try (ProgressMonitor.Task ignored = task.split(1).begin("Invoke 'ww2ogg'", 1)) {
                IOUtils.exec(settings.ww2oggPath, wemPath, "-o", oggPath, "--pcb", settings.ww2oggCodebooksPath);
            }

            try (ProgressMonitor.Task ignored = task.split(1).begin("Invoke 'revorb'", 1)) {
                IOUtils.exec(settings.revorbPath, oggPath);
            }

            try (ProgressMonitor.Task ignored = task.split(1).begin("Invoke 'ffmpeg'", 1)) {
                IOUtils.exec(settings.ffmpegPath, "-acodec", "vorbis", "-i", oggPath, "-ac", "2", wavPath, "-y");
            }

            return readClip(wavPath.toFile());
        } finally {
            Files.deleteIfExists(wemPath);
            Files.deleteIfExists(oggPath);
            Files.deleteIfExists(wavPath);
        }
    }

    @NotNull
    private static Clip readWaveClip(@NotNull ProgressMonitor monitor, @NotNull String codec, @NotNull byte[] data) throws Exception {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();
        final Path srcPath = Files.createTempFile(null, ".wave");
        final Path wavPath = Path.of(IOUtils.getBasename(srcPath.toString()) + ".wav");

        try (ProgressMonitor.Task task = monitor.begin("Read wave clip", 1)) {
            Files.write(srcPath, data);

            try (ProgressMonitor.Task ignored = task.split(1).begin("Invoke 'ffmpeg'", 1)) {
                IOUtils.exec(settings.ffmpegPath, "-acodec", codec, "-i", srcPath, "-ac", "2", wavPath, "-y");
            }

            return readClip(wavPath.toFile());
        } finally {
            Files.deleteIfExists(wavPath);
            Files.deleteIfExists(srcPath);
        }
    }

    @NotNull
    private static Clip readClip(@NotNull File file) throws Exception {
        try (AudioInputStream is = AudioSystem.getAudioInputStream(file)) {
            final AudioFormat format = is.getFormat();
            final DataLine.Info info = new DataLine.Info(Clip.class, format);
            final Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(is);

            return clip;
        }
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
