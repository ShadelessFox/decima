package com.shade.decima.ui.controls.audio;

import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;

public class AudioPlayer extends JPanel implements LineListener {
    private final PreviousTrackAction prevAction;
    private final NextTrackAction nextAction;
    private final PlayTrackAction playAction;
    private final JLabel currentTime;
    private final JLabel remainingTime;
    private final JProgressBar progressBar;
    private final Timer timer = new Timer(250, e -> updateProgress());

    private Clip clip;

    public AudioPlayer() {
        this.prevAction = new PreviousTrackAction();
        this.nextAction = new NextTrackAction();
        this.playAction = new PlayTrackAction();
        this.currentTime = new JLabel();
        this.remainingTime = new JLabel();

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true /* make it thicc */);
        progressBar.setString("");
        progressBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (clip == null || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                final var position = Math.min(Math.max(0, e.getPoint().x), progressBar.getWidth());
                final var microseconds = (long) ((double) position / progressBar.getWidth() * clip.getMicrosecondLength());

                clip.setMicrosecondPosition(microseconds);
                updateProgress();
            }
        });

        final JToolBar toolbar = new JToolBar();
        toolbar.setBorder(null);
        toolbar.add(currentTime);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(prevAction);
        toolbar.add(playAction);
        toolbar.add(nextAction);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(remainingTime);

        setLayout(new MigLayout("ins panel,gap 0", "[grow,fill]"));
        add(toolbar, "wrap");
        add(progressBar);

        updateState();
    }

    @Override
    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.START) {
            timer.start();
            playAction.setPlaying(true);
        } else if (event.getType() == LineEvent.Type.STOP) {
            timer.stop();
            playAction.setPlaying(false);
        }

        updateProgress();
    }

    public void setClip(@Nullable Clip clip) {
        if (clip != this.clip) {
            final Clip oldClip = this.clip;
            this.clip = clip;

            if (oldClip != null) {
                oldClip.removeLineListener(this);
                oldClip.stop();
            }

            firePropertyChange("clip", oldClip, clip);
            updateState();

            if (clip != null) {
                clip.addLineListener(this);
                start();
            }
        }
    }

    public void start() {
        if (clip == null) {
            return;
        }

        if (clip.getMicrosecondPosition() == clip.getMicrosecondLength()) {
            clip.setMicrosecondPosition(0);
        }

        clip.start();
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
        }
    }

    protected boolean previousTrackRequested() {
        return false;
    }

    protected boolean nextTrackRequested() {
        return false;
    }

    private void updateState() {
        if (clip == null) {
            prevAction.setEnabled(false);
            playAction.setEnabled(false);
            nextAction.setEnabled(false);
        } else {
            prevAction.setEnabled(true);
            playAction.setEnabled(true);
            nextAction.setEnabled(true);
        }

        updateProgress();
    }

    private void updateProgress() {
        if (clip == null) {
            setCurrentTime(Duration.ZERO);
            setRemainingTime(Duration.ZERO);
            setProgress(0);
        } else {
            setCurrentTime(Duration.ofMillis(clip.getMicrosecondPosition() / 1000));
            setRemainingTime(Duration.ofMillis(clip.getMicrosecondLength() / 1000));
            setProgress((double) clip.getMicrosecondPosition() / clip.getMicrosecondLength());
        }
    }

    private void setCurrentTime(@NotNull Duration duration) {
        currentTime.setText(getFormattedDuration(duration));
    }

    private void setRemainingTime(@NotNull Duration duration) {
        remainingTime.setText(getFormattedDuration(duration));
    }

    private void setProgress(double progress) {
        progressBar.setValue((int) (progress * 100));
    }

    @NotNull
    private static String getFormattedDuration(@NotNull Duration duration) {
        return "%d:%02d".formatted(duration.toMinutes(), duration.toSecondsPart());
    }

    private class PreviousTrackAction extends AbstractAction {
        public PreviousTrackAction() {
            putValue(SMALL_ICON, UIManager.getIcon("Toolbar.previousIcon"));
            putValue(SHORT_DESCRIPTION, "Previous");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (clip.getMicrosecondPosition() >= 500_000 || !previousTrackRequested()) {
                clip.setMicrosecondPosition(0);
                start();
            }
        }
    }

    private class NextTrackAction extends AbstractAction {
        public NextTrackAction() {
            putValue(SMALL_ICON, UIManager.getIcon("Toolbar.nextIcon"));
            putValue(SHORT_DESCRIPTION, "Next");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!nextTrackRequested()) {
                clip.setMicrosecondPosition(clip.getMicrosecondLength());
            }
        }
    }

    private class PlayTrackAction extends AbstractAction {
        private boolean playing;

        public PlayTrackAction() {
            setPlaying(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (playing) {
                stop();
            } else {
                start();
            }
        }

        public void setPlaying(boolean playing) {
            if (playing) {
                putValue(SMALL_ICON, UIManager.getIcon("Toolbar.pauseIcon"));
                putValue(SHORT_DESCRIPTION, "Pause");
            } else {
                putValue(SMALL_ICON, UIManager.getIcon("Toolbar.playIcon"));
                putValue(SHORT_DESCRIPTION, "Play");
            }

            this.playing = playing;
        }
    }
}
