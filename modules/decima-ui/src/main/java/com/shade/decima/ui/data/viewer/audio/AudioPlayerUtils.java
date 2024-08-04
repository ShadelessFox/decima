package com.shade.decima.ui.data.viewer.audio;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.data.viewer.audio.settings.AudioPlayerSettings;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

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

    @NotNull
    public static Duration getDuration(long sampleCount, long sampleRate) {
        return Duration.ofMillis(sampleCount * 1000L / sampleRate);
    }

    public static void extractTrack(
        @NotNull ProgressMonitor monitor,
        @NotNull Project project,
        @NotNull Playlist playlist,
        int index,
        @NotNull Path output
    ) throws IOException, InterruptedException {
        try (var task = monitor.begin("Extract track", 2)) {
            final byte[] data;

            try (var ignored = task.split(1).begin("Extract track data")) {
                data = playlist.getData(project.getPackfileManager(), index);
            }

            final Codec codec = playlist.getCodec(index);

            if (codec instanceof Codec.Generic generic) {
                extractFromGeneric(task.split(1), data, output, generic.name());
            } else {
                extractFromWwise(task.split(1), data, output);
            }
        }
    }

    private static void extractFromWwise(
        @NotNull ProgressMonitor monitor,
        @NotNull byte[] data,
        @NotNull Path output
    ) throws IOException, InterruptedException {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();
        final Path wemPath = Files.createTempFile(null, ".wem");
        final Path oggPath = Path.of(IOUtils.getBasename(wemPath) + ".ogg");

        try (var task = monitor.begin("Read wwise audio", 3)) {
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

    private static void extractFromGeneric(
        @NotNull ProgressMonitor monitor,
        @NotNull byte[] data,
        @NotNull Path output,
        @NotNull String codec
    ) throws IOException, InterruptedException {
        Path path = Files.createTempFile(null, ".bin");

        try (var task = monitor.begin("Read " + codec + " audio", 1)) {
            Files.write(path, data);
            convertAudio(task.split(1), path, output, codec);
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static void convertAudio(
        @NotNull ProgressMonitor monitor,
        @NotNull Path input,
        @NotNull Path output,
        @NotNull String codec
    ) throws IOException, InterruptedException {
        AudioPlayerSettings settings = AudioPlayerSettings.getInstance();

        if (IOUtils.getExtension(output).equals(codec)) {
            try (var ignored = monitor.begin("Copy audio file")) {
                Files.copy(input, output);
            }
        } else {
            try (var ignored = monitor.begin("Re-encode audio file")) {
                IOUtils.exec(settings.ffmpegPath, "-acodec", codec, "-i", input, "-ac", "2", output, "-y");
            }
        }
    }
}
