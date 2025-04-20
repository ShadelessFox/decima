package com.shade.decima.ui.data.viewer.audio.controls;

import com.shade.platform.model.util.MathUtils;
import com.shade.platform.ui.util.UIUtils;
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

public class AudioPlayerComponent extends JPanel implements LineListener {
    private final PreviousTrackAction prevAction;
    private final NextTrackAction nextAction;
    private final PlayTrackAction playAction;
    private final JLabel currentTime;
    private final JLabel remainingTime;
    private final JProgressBar progressBar;
    private final Timer timer = new Timer(250, e -> updateProgress());

    private Clip clip;

    public AudioPlayerComponent() {
        this.prevAction = new PreviousTrackAction();
        this.nextAction = new NextTrackAction();
        this.playAction = new PlayTrackAction();
        this.currentTime = new JLabel("", SwingConstants.LEADING);
        this.remainingTime = new JLabel("", SwingConstants.TRAILING);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true /* make it thicc */);
        progressBar.setString("");

        Handler handler = new Handler();
        progressBar.addMouseListener(handler);
        progressBar.addMouseMotionListener(handler);

        final JToolBar toolbar = new JToolBar();
        toolbar.setBorder(null);
        toolbar.add(prevAction);
        toolbar.add(playAction);
        toolbar.add(nextAction);

        setLayout(new MigLayout("ins panel,gap 0", "grow,fill"));
        add(currentTime, "w 50%");
        add(toolbar);
        add(remainingTime, "w 50%,wrap");
        add(progressBar, "spanx");

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

    public void close() {
        if (clip != null) {
            clip.stop();
            clip.close();
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
        currentTime.setText(UIUtils.formatDuration(duration));
    }

    private void setRemainingTime(@NotNull Duration duration) {
        remainingTime.setText(UIUtils.formatDuration(duration));
    }

    private void setProgress(double progress) {
        progressBar.setValue((int) (progress * 100));
    }

    private class PreviousTrackAction extends AbstractAction {
        public PreviousTrackAction() {
            putValue(SMALL_ICON, UIManager.getIcon("Action.previousIcon"));
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
            putValue(SMALL_ICON, UIManager.getIcon("Action.nextIcon"));
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
                putValue(SMALL_ICON, UIManager.getIcon("Action.pauseIcon"));
                putValue(SHORT_DESCRIPTION, "Pause");
            } else {
                putValue(SMALL_ICON, UIManager.getIcon("Action.playIcon"));
                putValue(SHORT_DESCRIPTION, "Play");
            }

            this.playing = playing;
        }
    }

    private class Handler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            handle(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            handle(e);
        }

        private void handle(MouseEvent e) {
            if (clip == null || !SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            float position = MathUtils.clamp(e.getPoint().x, 0.0f, progressBar.getWidth());
            long microseconds = (long) ((double) position / progressBar.getWidth() * clip.getMicrosecondLength());

            clip.setMicrosecondPosition(microseconds);
            updateProgress();
        }
    }
}
