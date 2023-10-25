package com.shade.decima.ui.data.viewer.audio;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.data.viewer.audio.playlists.WavePlaylist;
import com.shade.decima.ui.data.viewer.audio.settings.AudioPlayerSettings;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioPlayerUtils {
    private AudioPlayerUtils() {
        // prevents instantiation
    }

    @NotNull
    public static Clip openClip(@NotNull File file) throws Exception {
        try (AudioInputStream is = AudioSystem.getAudioInputStream(file)) {
            final AudioFormat format = is.getFormat();
            final DataLine.Info info = new DataLine.Info(Clip.class, format);
            final Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(is);

            return clip;
        }
    }

    public static void extractTrack(@NotNull ProgressMonitor monitor, @NotNull Project project, @NotNull Playlist playlist, int index, @NotNull Path output) throws IOException, InterruptedException {
        try (var task = monitor.begin("Extract track", 2)) {
            final byte[] data;

            try (var ignored = task.split(1).begin("Extract track data")) {
                data = playlist.getData(project.getPackfileManager(), index);
            }

            if (playlist instanceof WavePlaylist wave) {
                extractFromWave(task.split(1), data, output, wave);
            } else {
                extractFromVorbis(task.split(1), data, output);
            }
        }
    }

    private static void extractFromVorbis(@NotNull ProgressMonitor monitor, @NotNull byte[] data, @NotNull Path output) throws IOException, InterruptedException {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();
        final Path wemPath = Files.createTempFile(null, ".wem");
        final Path oggPath = Path.of(IOUtils.getBasename(wemPath.toString()) + ".ogg");

        try (var task = monitor.begin("Read vorbis audio", 3)) {
            Files.write(wemPath, data);

            try (var ignored = task.split(1).begin("Invoke 'ww2ogg'")) {
                IOUtils.exec(settings.ww2oggPath, wemPath, "-o", oggPath, "--pcb", settings.ww2oggCodebooksPath);
            }

            try (var ignored = task.split(1).begin("Invoke 'revorb'")) {
                IOUtils.exec(settings.revorbPath, oggPath);
            }

            convertAudio(task.split(1), oggPath, output, "vorbis");
        } finally {
            Files.deleteIfExists(wemPath);
            Files.deleteIfExists(oggPath);
        }
    }

    private static void extractFromWave(@NotNull ProgressMonitor monitor, @NotNull byte[] data, @NotNull Path output, @NotNull WavePlaylist wave) throws IOException, InterruptedException {
        final Path wavPath = Files.createTempFile(null, ".wav");

        try (var task = monitor.begin("Read wave audio", 1)) {
            Files.write(wavPath, data);

            convertAudio(task.split(1), wavPath, output, wave.getCodec());
        } finally {
            Files.deleteIfExists(wavPath);
        }
    }

    private static void convertAudio(@NotNull ProgressMonitor monitor, @NotNull Path input, @NotNull Path output, @NotNull String codec) throws IOException, InterruptedException {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();

        try (var ignored = monitor.begin("Re-encode audio file")) {
            IOUtils.exec(settings.ffmpegPath, "-acodec", codec, "-i", input, "-ac", "2", output, "-y");
        }
    }
}
