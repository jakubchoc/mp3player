import assets.Song;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayer extends PlaybackListener {

    private static final Object playSignal = new Object();

    private Song currentSong;
    private AdvancedPlayer advancedPlayer;
    private MusicPlayerGUI musicPlayerGUI;
    private Boolean isPaused;
    private int currentFrame;
    private int currentTimeInMilli;
    private List<Song> playlist;
    private int currentPlaylistIndex;
    private boolean songFinished;
    private boolean pressedPrev, pressedNext;

    public MusicPlayer(MusicPlayerGUI musicPlayerGUI) {
        this.musicPlayerGUI = musicPlayerGUI;
        this.isPaused = false;
    }

    public void loadSong(Song song) {
        currentSong = song;
        playlist = null;
        if(!songFinished)
            stopSong();

        if (currentSong != null) {
            currentFrame = 0;
            currentTimeInMilli = 0;
            musicPlayerGUI.setPlaybackSliderValue(0);
            playCurrentSong();
        }
    }

    public void loadPlaylist(File playlistFile) {
        playlist = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(playlistFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String songPath;
            while ((songPath = bufferedReader.readLine()) != null) {
                Song song = new Song(songPath);
                playlist.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (playlist.size() > 0) {
            musicPlayerGUI.setPlaybackSliderValue(0);
            currentSong = playlist.get(0);
            currentFrame = 0;
            musicPlayerGUI.enablePauseButtonDisablePlayButton();
            musicPlayerGUI.updateSongTitleAndArtist(currentSong);
            musicPlayerGUI.updatePlaybackSlider(currentSong);

            playCurrentSong();
        }
    }

    public void playCurrentSong() {
        if (currentSong == null) return;
        try {
            FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            advancedPlayer = new AdvancedPlayer(bufferedInputStream);
            advancedPlayer.setPlayBackListener(this);
            // start music
            startMusicThread();
            startPlaybackSliderThread();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void pauseSong() {
        if (advancedPlayer != null) {
            isPaused = true;
            stopSong();
        }
    }

    public void stopSong() {
        if (advancedPlayer != null) {
            advancedPlayer.stop();
            advancedPlayer.close();
            advancedPlayer = null;
        }
    }

    public void previousSong() {
        if (playlist == null) return;
        if (currentPlaylistIndex < 1) return;
        pressedPrev = true;

        if (!songFinished)
            stopSong();

        currentPlaylistIndex--;
        currentSong = playlist.get(currentPlaylistIndex);
        currentFrame = 0;
        currentTimeInMilli = 0;
        musicPlayerGUI.enablePauseButtonDisablePlayButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlaybackSlider(currentSong);

        playCurrentSong();
    }

    public void nextSong() {
        if (playlist == null) return;
        if (currentPlaylistIndex + 1 > playlist.size() - 1) return;
        pressedNext = true;

        if (!songFinished)
            stopSong();

        currentPlaylistIndex++;
        currentSong = playlist.get(currentPlaylistIndex);
        currentFrame = 0;
        currentTimeInMilli = 0;
        musicPlayerGUI.enablePauseButtonDisablePlayButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlaybackSlider(currentSong);

        playCurrentSong();
    }

    private void startMusicThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isPaused) {
                        synchronized (playSignal) {
                            isPaused = false;
                            playSignal.notify();
                        }
                        advancedPlayer.play(currentFrame, Integer.MAX_VALUE);
                    } else {
                        advancedPlayer.play();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startPlaybackSliderThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isPaused) {
                    try {
                        synchronized (playSignal) {
                            playSignal.wait();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                while (!isPaused && !songFinished && !pressedNext && !pressedPrev) {
                    try {
                        currentTimeInMilli++;
                        int calculatedFrame = (int) ((double) currentTimeInMilli * 2.08 * currentSong.getFrameRatePerMilliseconds());
                        musicPlayerGUI.setPlaybackSliderValue(calculatedFrame);
                        Thread.sleep(1);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void playbackStarted(PlaybackEvent evt) {
        // this method gets called in the beginning of the song
        System.out.println("Playback Started");
        songFinished = false;
        pressedNext = false;
        pressedPrev = false;
    }

    @Override
    public void playbackFinished(PlaybackEvent evt) {
        // this method gets called when the song is finished or if the player gets closed
        System.out.println("Playback Finished");
        if (isPaused) {
            currentFrame += (int) ((double) evt.getFrame() * currentSong.getFrameRatePerMilliseconds());
        } else {
            if (pressedPrev || pressedNext) return;
            songFinished = true;
            if (playlist == null) {
                musicPlayerGUI.enablePlayButtonDisablePauseButton();
            } else {
                if (currentPlaylistIndex == playlist.size() -1) {
                    musicPlayerGUI.enablePlayButtonDisablePauseButton();
                } else {
                    nextSong();
                }
             }
        }
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public void setCurrentFrame(int frame) {
        currentFrame = frame;
    }

    public void setCurrentTimeInMilli(int milli) {
        currentTimeInMilli = milli;
    }
}
