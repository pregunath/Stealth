package Game;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class SoundManager {
    private static MediaPlayer bgmPlayer;
    private static MediaPlayer alertPlayer;
    private static MediaPlayer hideSound;
    private static MediaPlayer unhideSound;

    public static void initialize() {
        try {
            // Background music
            URL bgmResource = SoundManager.class.getResource("/resources/bgm.mp3");
            if (bgmResource != null) {
                bgmPlayer = new MediaPlayer(new Media(bgmResource.toString()));
                bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                bgmPlayer.setVolume(0.3);
            }
            
            // Sound effects
            URL alertResource = SoundManager.class.getResource("/resources/alert.wav");
            if (alertResource != null) {
                alertPlayer = new MediaPlayer(new Media(alertResource.toString()));
            }
            
            URL hideResource = SoundManager.class.getResource("/resources/hide.wav");
            if (hideResource != null) {
                hideSound = new MediaPlayer(new Media(hideResource.toString()));
            }
            
            URL unhideResource = SoundManager.class.getResource("/resources/unhide.wav");
            if (unhideResource != null) {
                unhideSound = new MediaPlayer(new Media(unhideResource.toString()));
            }
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }

    public static void playBGM() {
        if (bgmPlayer != null) bgmPlayer.play();
    }

    public static void playAlert() {
        if (alertPlayer != null) {
            alertPlayer.stop();
            alertPlayer.play();
        }
    }

    public static void playHideSound() {
        if (hideSound != null) {
            hideSound.stop();
            hideSound.play();
        }
    }

    public static void playUnhideSound() {
        if (unhideSound != null) {
            unhideSound.stop();
            unhideSound.play();
        }
    }

    public static void stopBGM() {
        if (bgmPlayer != null) bgmPlayer.stop();
    }
}